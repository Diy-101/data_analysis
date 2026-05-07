package com.tradingbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TradingService {

    @Value("${gemini.base-promt}")
    private String basePromt;

    private final BybitService bybitService;
    private final GeminiService geminiService;

    @Autowired
    public TradingService (BybitService bybitService, GeminiService geminiService) {
        this.bybitService = bybitService;
        this.geminiService = geminiService;
    }

    public String checkMarket(String tradingPair) {
        try {
            String BybitResponse = bybitService.getPrice(tradingPair);
            return geminiService.generateAIResponse(basePromt + BybitResponse);
        } catch (Exception e) {
            log.error("Ошибка анализа | pair={} error={}", tradingPair, e.getMessage());
            return null;
        }
    }
}
