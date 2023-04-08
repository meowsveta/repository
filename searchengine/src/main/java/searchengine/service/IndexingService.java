package searchengine.service;

import searchengine.config.Site;

import java.util.List;

public interface IndexingService {

     void startIndexing();
    void parsePages(Site siteConfig);

    searchengine.model.Site updateSite(Site siteConfig, boolean delete);

     void stopIndexing();
     void indexPage(String url);

}
