package com.tradingbot.service;

import com.tradingbot.api.BybitClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TradingService {

    @Autowired
    private BybitClient bybitClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void checkMarket() {
        try {
            String response = bybitClient.getPrice("BTCUSDT");
            Object json = objectMapper.readValue(response, Object.class);
            String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

            System.out.println("Ответ от Bybit: " + pretty);
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
