package com.tradingbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BybitService {

    @Value("${bybit.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
}
