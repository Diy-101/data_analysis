package com.tradingbot;

import com.tradingbot.schedular.Schedular;
import com.tradingbot.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TradingServiceTest {
    @MockitoBean
    private Schedular schedular;

    private final TradingService tradingService;

    @Autowired
    public TradingServiceTest(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @Test
    void testGetBalance() {
        String balance = tradingService.getBalance();
        System.out.println(balance);
        assertNotNull(balance);
    }

    @Test
    void testGetUsdtBalance() {
        Double usdtBalance = tradingService.getUsdtBalance();
        System.out.println("USDT: " + usdtBalance.toString());
        assertNotNull(usdtBalance);
    }
}
