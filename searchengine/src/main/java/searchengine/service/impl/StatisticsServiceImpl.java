package searchengine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        List<Site> sites = siteRepository.findAll();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.size());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : sites) {
            long pageCountBy = pageRepository.countBy(site.getId());
            long lemmaCountBy = lemmaRepository.countBy(site.getId());
            total.setPages(total.getPages() + pageCountBy)
                .setLemmas(total.getLemmas() + lemmaCountBy);
            if (!total.isIndexing() && site.getStatus() == Status.INDEXING) {
                total.setIndexing(true);
            }
            detailed.add(
                new DetailedStatisticsItem(site.getName(),
                        site.getUrl(),
                        site.getStatus().name(),
                        site.getStatusTime(),
                        site.getLastError() != null ? site.getLastError() : "",
                        pageCountBy,
                        lemmaCountBy)
            );
        }

        StatisticsData data = new StatisticsData();
        data.setTotal(total)
            .setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
