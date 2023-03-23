package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.ApiResponse;
import searchengine.dto.SearchResult;
import searchengine.model.*;
import searchengine.error.ErrorMessage;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.LemmaUtils;
import searchengine.utils.SnippetUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    public ApiResponse search(SearchFilter filter) {
        if (filter.getQuery() == null || filter.getQuery().trim().isBlank()) {
            throw new ErrorMessage("Поисковый запрос не может быть пустым");
        }
        String query = filter.getQuery().trim();
        ApiResponse response = new ApiResponse(true, 0, new ArrayList<>());
        Site site = searchSite(filter.getSite());
        HashMap<String, Integer> sourceLemmas = LemmaUtils.lemmatization(query, false);
        List<Lemma> existLemmas =
            site == null ?
                lemmaRepository.getByLemma(sourceLemmas.keySet()) :
                lemmaRepository.getByLemma(site.getId(), sourceLemmas.keySet());
        List<Page> matchPages = searchPages(existLemmas);
        if (matchPages.isEmpty()) {
            return response;
        }
        List<SearchResult> result = computeRelevance(
            matchPages, existLemmas, sourceLemmas.keySet()
        );
        return response
            .setCount(result.size())
            .setData(result);
    }

    private Site searchSite(String url) {
        Site site = null;
        if (url != null && !url.isBlank()) {
            url = url.endsWith("/") ?
                url.substring(0, url.length() - 1) : url;
            site = siteRepository.getByUrl(url.trim());
            if (site == null) {
                throw new ErrorMessage("Сайт не найден");
            }
            if (site.getStatus() != Status.INDEXED) {
                throw new ErrorMessage(
                    "Сайт не проиндексирован: " + site.getStatus()
                );
            }
        }
        return site;
    }

    private List<Page> searchPages(List<Lemma> existLemmas) {
        List<Page> matchPages = new ArrayList<>();
        for (int i = 0; i < existLemmas.size(); i++) {
            Lemma lemma = existLemmas.get(i);
            if (i == 0) {
                matchPages = pageRepository.getByLemma(lemma.getId());
            } else {
                List<Long> ids = getIds(matchPages);
                matchPages = pageRepository.getByLemma(
                    lemma.getId(), ids
                );
            }
            if (matchPages.isEmpty()) {
                break;
            }
        }
        return matchPages;
    }

    private List<SearchResult> computeRelevance(List<Page> pages,
                                                List<Lemma> existLemmas,
                                                Set<String> sourceLemmas) {
        List<SearchResult> result = new ArrayList<>();
        List<Long> pageIds = getIds(pages);
        int totalRelevance = indexRepository.totalRelevance(pageIds);
        Map<Long, Double> relevanceByLemmas = indexRepository
            .relevanceByLemmas(pageIds, getIds(existLemmas))
            .stream()
            .collect(Collectors.toMap(
                o -> (Long) o[0], o -> (Double) o[1]
            ));
        for (Page page : pages) {
            Double pageRelevance = relevanceByLemmas.get(page.getId());
            if (pageRelevance == null || pageRelevance == 0) {
                continue;
            }
            Site site = page.getSite();
            Document document = Jsoup.parse(page.getContent());
            String snippet = SnippetUtils.generateSnippet(document, sourceLemmas);
            result.add(
                new SearchResult(
                    site.getUrl(), site.getName(),
                    page.getPath(), document.title(),
                    snippet, pageRelevance / totalRelevance
                )
            );
        }
        return result;
    }

    private <T> List<Long> getIds(List<T> objects) {
        return objects.stream().map(
            obj -> {
                if (obj instanceof Page) {
                    return ((Page) obj).getId();
                } else if (obj instanceof Lemma) {
                    return ((Lemma) obj).getId();
                }
                throw new ErrorMessage("Неподдерживаемый тип");
            }
        ).toList();
    }

    public double searchrelevance(List<Page> pages, List<Lemma> existLemmas) {
//        List<SearchResult> result = new ArrayList<>();
        List<Long> pageIds = getIds(pages);
        int totalRelevance = indexRepository.totalRelevance(pageIds);
        double relevance = 0;
        Map<Long, Double> relevanceByLemmas = indexRepository
                .relevanceByLemmas(pageIds, getIds(existLemmas))
                .stream()
                .collect(Collectors.toMap(
                        o -> (Long) o[0], o -> (Double) o[1]
                ));
        for (Page page : pages) {
            Double pageRelevance = relevanceByLemmas.get(page.getId());
            if (pageRelevance == null || pageRelevance == 0) {
                continue;
            }
            relevance = pageRelevance / totalRelevance;
        }
        return relevance;
    }


}
