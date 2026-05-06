package com.tradingbot.schedular;

import com.tradingbot.service.TradingService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class Schedular {

    private final TradingService tradingService;

    public Schedular(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @Scheduled(fixedDelay = 10000) // каждые 10 секунд
    public void run() {
        tradingService.checkMarket();
    }
}
