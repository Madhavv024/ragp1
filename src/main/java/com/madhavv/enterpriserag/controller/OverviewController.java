package com.madhavv.enterpriserag.controller;

import com.madhavv.enterpriserag.dto.OverviewResponse;
import com.madhavv.enterpriserag.service.OverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    public ResponseEntity<OverviewResponse> getOverview() {
        return ResponseEntity.ok(overviewService.getOverview());
    }
}