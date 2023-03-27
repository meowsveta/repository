package searchengine.service;

import searchengine.dto.ApiResponse;
import searchengine.dto.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchFilter;
import searchengine.model.Site;

import java.util.List;
import java.util.Set;

public interface SearchService {
    ApiResponse search(SearchFilter filter);
    Site searchSite(String url);
    List<Page> searchPages(List<Lemma> existLemmas);
     List<SearchResult> computeRelevance(List<Page> pages,
                                               List<Lemma> existLemmas,
                                               Set<String> sourceLemmas);
    double searchRelevance(List<Page> pages, List<Lemma> existLemmas);


}
