package com.tradingbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BybitService {

    @Value("${bybit.api-key}")
    private String apiKey;
    @Value("${bybit.base-url}")
    private String BASE_URL;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getPrice(String symbol) {
        String url = BASE_URL + "/v5/market/tickers?category=spot&symbol=" + symbol;
        return restTemplate.getForObject(url, String.class);
    }
}
