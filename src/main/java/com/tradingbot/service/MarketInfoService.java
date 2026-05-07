package com.tradingbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketInfoService {
    private final RestTemplate restTemplate = new RestTemplate();

    public String getFearAndGreedIndex() {
        String url = "https://api.alternative.me/fng/";
        return restTemplate.getForObject(url, String.class);
    }
}
