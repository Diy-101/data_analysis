package com.tradingbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MarketInfoService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${bybit.base-url}")
    private String BASE_URL;

    public String getFearAndGreedIndex() {
        String url = "https://api.alternative.me/fng/";
        return restTemplate.getForObject(url, String.class);
    }

    public String getPrice(String symbol) {
        String url = BASE_URL + "/v5/market/tickers?category=spot&symbol=" + symbol;
        String data = restTemplate.getForObject(url, String.class);
        return data;
    }

    public String candlesPerHours(String symbol, String hours) {
        String url = BASE_URL + "/v5/market/kline?category=spot&symbol="+ symbol +"&interval=60&limit=" + hours;
        String data = restTemplate.getForObject(url, String.class);
        return data;
    }

    public String candlesPer24Hours(String symbol) {
        String url = BASE_URL + "/v5/market/kline?category=spot&symbol="+ symbol +"&interval=60&limit=24";
        String data = restTemplate.getForObject(url, String.class);
        return data;
    }

    public String longShortRationPerHours(String symbol, String hours) {
        String url = BASE_URL + "/v5/market/account-ratio?category=linear&symbol=" + symbol + "&period=1h&limit=" + hours;
        String data = restTemplate.getForObject(url, String.class);
        return data;
    }

    public String longShortRationPer24Hours(String symbol) {
        String url = BASE_URL + "/v5/market/account-ratio?category=linear&symbol=" + symbol + "&period=1h&limit=24";
        String data = restTemplate.getForObject(url, String.class);
        return data;
    }

    public String getKlines(String symbol, String interval, int limit) {
        String url = BASE_URL + "/v5/market/kline?category=spot&symbol=" + symbol
                + "&interval=" + interval
                + "&limit=" + limit;
        return restTemplate.getForObject(url, String.class);
    }

    public String getFundingRate(String symbol) {
        String url = BASE_URL + "/v5/market/funding/history?category=linear&symbol="
                + symbol + "&limit=3";
        return restTemplate.getForObject(url, String.class);
    }
}
