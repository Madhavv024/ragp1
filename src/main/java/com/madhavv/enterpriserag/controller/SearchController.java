package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.SearchRequestDto;
import com.madhavv.enterpriserag.dto.SearchResult;
import com.madhavv.enterpriserag.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public List<SearchResult> search(@RequestBody SearchRequestDto request) {
        return searchService.search(request);
    }
}