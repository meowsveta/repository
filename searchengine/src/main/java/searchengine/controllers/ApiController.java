package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ApiResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SearchFilter;
import searchengine.service.impl.IndexingServiceImpl;
import searchengine.service.impl.SearchServiceImpl;
import searchengine.service.impl.StatisticsServiceImpl;

@RestController
@RequestMapping("/api/")
public class ApiController extends CommonController {

    @Autowired
    private StatisticsServiceImpl statisticsServiceImpl;
    @Autowired
    private IndexingServiceImpl indexingServiceImpl;
    @Autowired
    private SearchServiceImpl searchServiceImpl;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsServiceImpl.getStatistics();
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        indexingServiceImpl.startIndexing();
        return okResponse();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        indexingServiceImpl.stopIndexing();
        return okResponse();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        indexingServiceImpl.indexPage(url);
        return okResponse();
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(SearchFilter filter) {
        return new ResponseEntity<>(searchServiceImpl.search(filter), HttpStatus.OK);
    }
}
