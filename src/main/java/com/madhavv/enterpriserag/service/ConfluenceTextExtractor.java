package com.madhavv.enterpriserag.service;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class ConfluenceTextExtractor {

    public String extract(String html) {
        return Jsoup.parse(html)
                .text();
    }
}