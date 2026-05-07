package com.tradingbot.schedular;

import com.tradingbot.service.MarketInfoService;
import com.tradingbot.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class Schedular {

    @Value("${bybit.trading-pair}")
    private String tradingPair;

    private final TradingService tradingService;
    private final MarketInfoService marketInfoService;

    @Autowired
    public Schedular(TradingService tradingService, MarketInfoService marketInfoService) {
        this.tradingService = tradingService;
        this.marketInfoService = marketInfoService;
    }

    @Scheduled(fixedDelay = 10000) // каждые 10 секунд
    public void run() {
        System.out.println("=== Scheduler запустился ===");
        System.out.println(tradingService.checkMarket(tradingPair));
        System.out.println(marketInfoService.getFearAndGreedIndex());
    }
}
