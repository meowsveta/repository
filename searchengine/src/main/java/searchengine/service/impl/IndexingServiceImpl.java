package searchengine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Status;
import searchengine.error.ErrorMessage;
import searchengine.repository.*;
import searchengine.service.IndexingService;
import searchengine.utils.PageRecursiveTask;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private final Executor executor = Executors.newFixedThreadPool(PROCESSORS);
    private List<PageRecursiveTask> tasks = new ArrayList<>();

    @Autowired
    private SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;


    @Override
    public void startIndexing() {
        if (!tasks.isEmpty()) {
            throw new ErrorMessage("Индексация уже запущена");
        }
        List<Site> sitesList = getSites();
        for (Site site : sitesList) {
            checkSiteConfig(site);
            executor.execute(
                () -> parsePages(site)
            );
        }
    }

    @Override
    public void parsePages(Site siteConfig) {
        searchengine.model.Site site = updateSite(siteConfig, true);
        PageRecursiveTask task = new PageRecursiveTask(
            site, siteConfig.getUrl(),
            siteRepository, pageRepository,
            lemmaRepository, indexRepository);
        tasks.add(task);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.submit(task);
        if (task.join()) {
            siteRepository.updateStatus(Status.INDEXED.name(), site.getId());
        }
        tasks.clear();
    }

    @Override
    public searchengine.model.Site updateSite(Site siteConfig, boolean delete) {
        String url = siteConfig.getUrl();
        searchengine.model.Site site = siteRepository.getByUrl(url);
        if (site == null) {
            site = new searchengine.model.Site(
                siteConfig.getUrl(), siteConfig.getName(),
                Status.INDEXING, LocalDateTime.now(), ""
            );
            return siteRepository.saveAndFlush(site);
        } else if (delete) {
            indexRepository.deleteBySiteId(site.getId());
            lemmaRepository.deleteBySiteId(site.getId());
            pageRepository.deleteBySiteId(site.getId());
            site.setName(siteConfig.getName())
                .setStatus(Status.INDEXING)
                .setStatusTime(LocalDateTime.now())
                .setLastError("");
            return siteRepository.saveAndFlush(site);
        }
        if (site.getId() == null) {
            throw new ErrorMessage("Сайт не найден");
        }
        return site;
    }

    @Override
    public void stopIndexing() {
        if (tasks.isEmpty()) {
            throw new ErrorMessage("Индексация не запущена");
        }
        for (PageRecursiveTask task : tasks) {
            if (!task.isCancelled()) {
                task.stopIndexing();
            }
        }
    }

    @Override
    public void indexPage(String url) {
        boolean outsideUrl = true;
        for (Site siteConfig : getSites()) {
            String parentUrl = siteConfig.getUrl().trim();
            if (parentUrl.isBlank()) {
                continue;
            }
            if (url.startsWith(parentUrl)) {
                outsideUrl = false;
                executor.execute(
                    () -> {
                        searchengine.model.Site site = updateSite(siteConfig, false);
                        new PageRecursiveTask(
                            site, url, siteRepository,
                                pageRepository, lemmaRepository,
                                 indexRepository,
                            url.equals(parentUrl), parentUrl
                        ).indexPage();
                    }
                );
                break;
            }
        }
        if (outsideUrl) {
            throw new ErrorMessage("Неизвестный URL");
        }
    }

    private List<Site> getSites() {
        List<Site> sitesList = sites.getSites();
        if (CollectionUtils.isEmpty(sitesList)) {
            throw new RuntimeException("Список сайтов не может быть пустым");
        }
        return sitesList;
    }


    private void checkSiteConfig(Site site) {
        if (site.getUrl().isBlank()) {
            throw new RuntimeException("URL не может быть пустым");
        }
        if (site.getName().isBlank()) {
            throw new RuntimeException("Название сайта не может быть пустым");
        }
    }

}
