package com.tradingbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import jakarta.annotation.PostConstruct;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TradingService {

    @Value("${gemini.base-prompt}")
    private Resource basePromptResource;
    @Value("${bybit.api-key}")
    private String apiKey;
    @Value("${bybit.secret}")
    private String secret;
    @Value("${bybit.base-url-trading}")
    private String baseUrlTrading;
    @Value("${trading.order-size-usdt}")
    private double orderSizeUsdt;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String basePrompt;
    private final GeminiService geminiService;
    private final MarketInfoService marketInfoService;
    private final IndicatorsService indicatorsService;

    @Autowired
    public TradingService (GeminiService geminiService, MarketInfoService marketInfoService, IndicatorsService indicatorsService) {
        this.geminiService = geminiService;
        this.marketInfoService = marketInfoService;
        this.indicatorsService = indicatorsService;
    }

    @PostConstruct
    public void init() throws IOException {
        basePrompt = basePromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String checkMarket(String tradingPair) {
        try {
            String ticker = marketInfoService.getPrice(tradingPair);
            String indicators1h = indicatorsService.getIndicators(tradingPair, "60");
            String indicators4h = indicatorsService.getIndicators(tradingPair, "240");
            String indicators1d = indicatorsService.getIndicators(tradingPair, "D");
            String fearGreed = marketInfoService.getFearAndGreedIndex();
            String longShort = marketInfoService.longShortRationPer24Hours(tradingPair);
            String fundingRate = marketInfoService.getFundingRate(tradingPair);

            String marketIndicators = """
                    
                    === РЫНОЧНЫЕ ДАННЫЕ ===
                    %s
                    
                    === НАСТРОЕНИЕ РЫНКА ===
                    Fear & Greed: %s
                    Long/Short Ratio: %s
                    Funding Rate: %s
                    
                    === ТЕХНИЧЕСКИЙ АНАЛИЗ (1 ДЕНЬ) ===
                    %s
                    
                    === ТЕХНИЧЕСКИЙ АНАЛИЗ (4 ЧАСА) ===
                    %s
                    
                    === ТЕХНИЧЕСКИЙ АНАЛИЗ (1 ЧАС) ===
                    %s
                    """.formatted(ticker, fearGreed, longShort, fundingRate,
                    indicators1d, indicators4h, indicators1h);
            String fullPrompt = basePrompt + marketIndicators;
            String AIResponse = geminiService.generateAIResponse(fullPrompt);

            return """
                    === Текущая цена ===
                    %s
                    
                    === Индикаторы ===
                    %s
                    
                    === Ответ ИИ ===
                    %s
                    """.formatted(ticker, marketIndicators, AIResponse);
        } catch (Exception e) {
            return null;
        }
    }

    // Подпись запроса
    private String generateSignature(String timestamp, String payload) throws Exception {
        String data = timestamp + apiKey + "5000" + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
    }

    // Универсальный метод для POST запросов с подписью
    private String signedPost(String endpoint, Map<String, Object> body) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String payload = objectMapper.writeValueAsString(body);
        String signature = generateSignature(timestamp, payload);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-BAPI-API-KEY", apiKey);
        headers.set("X-BAPI-TIMESTAMP", timestamp);
        headers.set("X-BAPI-SIGN", signature);
        headers.set("X-BAPI-RECV-WINDOW", "5000");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        return restTemplate.postForObject(baseUrlTrading + endpoint, entity, String.class);
    }

    // Купить BTC (открыть лонг)
    public String buy(String symbol, double price) {
        try {
            // считаем количество BTC на наш бюджет
            double qty = orderSizeUsdt / price;
            // округляем до 6 знаков
            String qtyStr = String.format("%.6f", qty);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("category", "spot");
            body.put("symbol", symbol);
            body.put("side", "Buy");
            body.put("orderType", "Market");
            body.put("qty", qtyStr);

            String response = signedPost("/v5/order/create", body);
            return response;

        } catch (Exception e) {
            return null;
        }
    }

    // Продать BTC (закрыть лонг)
    public String sell(String symbol, double qty) {
        try {
            String qtyStr = String.format("%.6f", qty);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("category", "spot");
            body.put("symbol", symbol);
            body.put("side", "Sell");
            body.put("orderType", "Market");
            body.put("qty", qtyStr);

            String response = signedPost("/v5/order/create", body);
            return response;

        } catch (Exception e) {
            return null;
        }
    }

    // Получить баланс
    public String getBalance() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String queryString = "accountType=UNIFIED";
            String signature = generateSignature(timestamp, queryString);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-BAPI-API-KEY", apiKey);
            headers.set("X-BAPI-TIMESTAMP", timestamp);
            headers.set("X-BAPI-SIGN", signature);
            headers.set("X-BAPI-RECV-WINDOW", "5000");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = baseUrlTrading + "/v5/account/wallet-balance?" + queryString;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return response.getBody();

        } catch (Exception e) {
            return null;
        }
    }

    public double getUsdtBalance() {
        try {
            String response = getBalance();
            JsonNode coins = objectMapper.readTree(response)
                    .path("result")
                    .path("list")
                    .get(0)
                    .path("coin");

            for (JsonNode coin : coins) {
                if (coin.path("coin").asText().equals("USDT")) {
                    return coin.path("walletBalance").asDouble();
                }
            }
        } catch (Exception e) {

        }
        return 0;
    }
}
