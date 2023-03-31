package searchengine.utils;

import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.error.ErrorMessage;
import searchengine.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@AllArgsConstructor
public class PageRecursiveTask extends RecursiveTask<Boolean> {

    private static final int BAD_CODE = 400;
    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);
    private static final Pattern EXCESS_LINK = Pattern.compile("(png|pdf|jpg|gif|#)");
    private static List<PageRecursiveTask> tasks = new ArrayList<>();
    private static boolean runIndexing = true;

    public PageRecursiveTask(Site site, String url,
                             SiteRepository siteRepository,
                             PageRepository pageRepository,
                             LemmaRepository lemmaRepository,
                             IndexRepository indexRepository) {
        this.site = site;
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    private final Site site;
    private final String url;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private boolean isFirst = true;
    private String firstUrl;

    public void stopIndexing() {
        runIndexing = false;
    }

    @Override
    protected Boolean compute() {
        try {
            Thread.sleep(TIMEOUT_MILLIS);
            Connection.Response response = getResponse(url);
            Document document = response.parse();

            String formatUrl = getFormatUrl(url);
            if (formatUrl.isBlank()) {
                return runIndexing;
            }
            update(formatUrl, document, response);
            parseChildren(document);
            Page page = pageRepository.getByPath(formatUrl, site.getId());
            if (page == null) {
                throw new ErrorMessage("Страница не найдена");
            }
            appendLemma(page, document);
        } catch (Exception ex) {
            if (!runIndexing) {
                siteRepository.updateFailedStatus(
                        Status.FAILED.name(), ex.getMessage(), site.getId()
                );
            } else {
                siteRepository.updateLastError(ex.getMessage(), site.getId());
            }
        }
        return runIndexing;
    }

    public void update(String formatUrl, Document document, Connection.Response response ){
        if (isFirst) {
            pageRepository.insert(
                    site.getId(), formatUrl,
                    response.statusCode(), document.html()
            );
            isFirst = false;
        } else {
            int update = pageRepository.update(
                    response.statusCode(), document.html(), site.getId(), formatUrl
            );
            if (update < 1) {
                throw new ErrorMessage("Страница не обновлена");
            }
            siteRepository.updateStatusTime(LocalDateTime.now(), site.getId());
        }

    }

    private void parseChildren(Document document) {
        Elements links = document.select("a");
        for (Element element : links) {
            String absUrl = element.absUrl("href");
            if (isCorrectUrl(absUrl)) {
                String formatUrl = getFormatUrl(absUrl);
                if (formatUrl.isBlank()) {
                    continue;
                }
                int insert = pageRepository.insert(site.getId(), formatUrl);
                if (insert < 1) {
                    continue;
                }
                if (runIndexing) {
                    PageRecursiveTask task = new PageRecursiveTask(
                            site, absUrl, siteRepository,
                            pageRepository, lemmaRepository,
                            indexRepository,
                            false, firstUrl
                    );
                    tasks.add(task);
                    task.fork();
                    task.join();
                } else {
                    throw new ErrorMessage("Индексация остановлена пользователем");
                }
            }
        }
    }

    public String indexPage() {
        try {
            Connection.Response response = getResponse(url);
            Document document = response.parse();
            String formatUrl = getFormatUrl(url);
            if (formatUrl.isBlank()) {
                throw new ErrorMessage("Пустой URL");
            }
            removePage(formatUrl);
            Page page = pageRepository.saveAndFlush(
                    new Page()
                            .setSite(site)
                            .setPath(formatUrl)
                            .setCode(response.statusCode())
                            .setContent(document.html())
            );
            appendLemma(page, document);
            return "ok";
        } catch (Exception ex) {
            throw new ErrorMessage(ex.getMessage());
        }
    }

    private void removePage(String url) {
        Long pageId = pageRepository.getIdByPath(url, site.getId());
        if (pageId != null) {
            lemmaRepository.updateByPage(pageId);
            indexRepository.deleteByPageId(pageId);
            pageRepository.deleteById(pageId);
        }
    }

    private void appendLemma(Page page, Document document) {
        if (page.getCode() >= BAD_CODE) {
            return;
        }
        try {
            HashMap<String, Integer> lemmasRaw = LemmaUtils.lemmatization(document);
            if (lemmasRaw.isEmpty()) {
                return;
            }
            List<Lemma> lemmas = new ArrayList<>();
            List<Index> indices = new ArrayList<>();
            lemmasRaw.forEach(
                    (lemma, count) -> {
                        lemmaRepository.insertLemma(site.getId(), lemma, count);
                        lemmas.add(new Lemma(site, lemma, 1));
                    } );

            List<Lemma> lemmaSaved = lemmaRepository.findAllBySiteIdAndLemmaInOrderByFrequency(
                    site.getId(), lemmasRaw.keySet()
            );
            for (Lemma lemma : lemmaSaved) {
                Double count = Double.valueOf(lemmasRaw.get(lemma.getLemma()));
                indexRepository.insertIndex(lemma.getId(), page.getId(), count);
                if (count == null) {
                    continue;
                }
                indices.add(
                        new Index(page, lemma, count)
                );
            }
        } catch (Exception ex) {
            throw new ErrorMessage("Ошибка лемматизации");
        }
    }

    private Connection.Response getResponse(String url) {
        try {
            return Jsoup
                    .connect(url)
                    .ignoreHttpErrors(true)
                    .userAgent("SearchEngineBot")
                    .referrer("https://www.google.com")
                    .execute();
        } catch (Exception ex) {
            throw new ErrorMessage("Неуспешное соединение");
        }
    }

    private synchronized String getFormatUrl(String url) {
        String slash = "/";
        url = url.trim();
        if (isFirst) {
            firstUrl = url.endsWith(slash) ?
                    url.substring(0, url.length() - 1) : url;
            return "/";
        }
        if (firstUrl == null) {
            throw new ErrorMessage("Начальный URL не может быть пустым");
        }
        return url.startsWith(firstUrl) ?
                url.substring(firstUrl.length()) : url;
    }

    private boolean isCorrectUrl(String url) {
        return (url.startsWith("/") || url.startsWith(this.url)) &&
                !EXCESS_LINK.matcher(url).find();
    }
}