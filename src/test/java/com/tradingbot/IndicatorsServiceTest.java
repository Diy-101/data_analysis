package com.tradingbot;

import com.tradingbot.schedular.Schedular;
import com.tradingbot.service.IndicatorsService;
import com.tradingbot.service.MarketInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class IndicatorsServiceTest {
    private final IndicatorsService indicatorsService;

    @Value("${bybit.trading-pair}")
    private String tradingPair;
    @MockitoBean
    private Schedular schedular;

    @Autowired
    public IndicatorsServiceTest(IndicatorsService indicatorsService) {
        this.indicatorsService = indicatorsService;
    }

    @Test
    void testGetIndicators () {
       String indicators1h = indicatorsService.getIndicators(tradingPair, "60");
       String indicators4h = indicatorsService.getIndicators(tradingPair, "240");
       String indicators1d = indicatorsService.getIndicators(tradingPair, "D");
    }
}
