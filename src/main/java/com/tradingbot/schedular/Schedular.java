package com.tradingbot.schedular;

import com.tradingbot.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class Schedular {

    @Autowired
    private TradingService tradingService;

    @Scheduled(fixedDelay = 10000) // каждые 10 секунд
    public void run() {
        tradingService.checkMarket();
    }
}
