package com.tradingbot.service;

import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MedianPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuKijunSenIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanAIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanBIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuTenkanSenIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class IndicatorsService {

    private final MarketInfoService marketInfoService;
    private final ObjectMapper objectMapper;

    public IndicatorsService(MarketInfoService marketInfoService, ObjectMapper objectMapper) {
        this.marketInfoService = marketInfoService;
        this.objectMapper = objectMapper;
    }

    public String getIndicators(String symbol, String interval) {
        try {
            BarSeries series = buildSeries(symbol, interval);
            int lastIndex = series.getEndIndex();
            double currentPrice = new ClosePriceIndicator(series)
                    .getValue(lastIndex).doubleValue();

            String oscillators = calculateOscillators(series, lastIndex, currentPrice);
            String movingAvgs  = calculateMovingAverages(series, lastIndex, currentPrice);
            String pivots      = calculatePivotPoints(series, lastIndex);
            String nw = calculateNadarayaWatson(series, lastIndex);


            String result = """
                === ИНДИКАТОРЫ [%s] ===
                Цена: %.2f
                
                %s
                %s
                %s
                %s
                """.formatted(interval, currentPrice, oscillators, movingAvgs, pivots, nw);

            return result;

        } catch (Exception e) {
            return "Индикаторы недоступны";
        }
    }

    // Oscillators
    private String calculateOscillators(BarSeries series, int lastIndex, double currentPrice) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);

        double rsi      = new RSIIndicator(close, 14).getValue(lastIndex).doubleValue();
        double macd     = new MACDIndicator(close, 12, 26).getValue(lastIndex).doubleValue();
        double adx      = new ADXIndicator(series, 14).getValue(lastIndex).doubleValue();
        double atr      = new ATRIndicator(series, 14).getValue(lastIndex).doubleValue();
        double stochRsi = new StochasticRSIIndicator(close, 14).getValue(lastIndex).doubleValue();
        double cci      = new CCIIndicator(series, 20).getValue(lastIndex).doubleValue();
        double williamsR = new WilliamsRIndicator(series, 14).getValue(lastIndex).doubleValue();

        StochasticOscillatorKIndicator stochK = new StochasticOscillatorKIndicator(series, 14);
        double stochKValue = stochK.getValue(lastIndex).doubleValue();
        double stochDValue = new SMAIndicator(stochK, 3).getValue(lastIndex).doubleValue();

        double ao = new SMAIndicator(new MedianPriceIndicator(series), 5).getValue(lastIndex).doubleValue()
                - new SMAIndicator(new MedianPriceIndicator(series), 34).getValue(lastIndex).doubleValue();

        double momentum  = new ROCIndicator(close, 10).getValue(lastIndex).doubleValue();

        EMAIndicator ema13 = new EMAIndicator(close, 13);
        double bullPower = series.getBar(lastIndex).getHighPrice().doubleValue() - ema13.getValue(lastIndex).doubleValue();
        double bearPower = series.getBar(lastIndex).getLowPrice().doubleValue()  - ema13.getValue(lastIndex).doubleValue();
        double bbPower   = bullPower + bearPower;

        // Анализ объёма
        VolumeIndicator volumeIndicator = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volumeIndicator, 20);
        double currentVolume = series.getBar(lastIndex).getVolume().doubleValue();
        double avgVol = avgVolume.getValue(lastIndex).doubleValue();
        double volumeRatio = currentVolume / avgVol;

        String volumeSignal = volumeRatio > 2.0 ? "🔥 аномально высокий"
                : volumeRatio > 1.5 ? "📈 высокий"
                  : volumeRatio < 0.5 ? "📉 низкий"
                    : "нормальный";

        // Дивергенция RSI
        RSIIndicator rsiIndicator = new RSIIndicator(close, 14);
        double prevPrice = series.getBar(lastIndex - 5).getClosePrice().doubleValue();
        double prevRsi   = rsiIndicator.getValue(lastIndex - 5).doubleValue();

        String divergence;
        if (currentPrice > prevPrice && rsi < prevRsi)
            divergence = "⚠️ медвежья дивергенция";
        else if (currentPrice < prevPrice && rsi > prevRsi)
            divergence = "⚠️ бычья дивергенция";
        else
            divergence = "нет";

        return """
            --- Осцилляторы ---
            RSI(14): %.2f %s
            MACD(12,26): %.2f %s
            CCI(20): %.2f %s
            ADX(14): %.2f %s
            ATR(14): %.2f
            Stochastic %%K/%%D: %.2f / %.2f %s
            Stochastic RSI: %.2f %s
            Williams %%R(14): %.2f %s
            Awesome Oscillator: %.2f %s
            Momentum(10): %.2f %s
            Bull Bear Power: %.2f %s
            Объём: %.2f (%.1fx от среднего) — %s
            Дивергенция RSI: %s
            """.formatted(
                rsi, rsiSignal(rsi),
                macd, macd > 0 ? "бычий" : "медвежий",
                cci, cci > 100 ? "перекуплен" : cci < -100 ? "перепродан" : "нейтрально",
                adx, adx > 25 ? "сильный тренд" : "боковик",
                atr,
                stochKValue, stochDValue, stochKValue > 80 ? "перекуплен" : stochKValue < 20 ? "перепродан" : "нейтрально",
                stochRsi * 100, stochRsiSignal(stochRsi),
                williamsR, williamsR < -80 ? "перепродан" : williamsR > -20 ? "перекуплен" : "нейтрально",
                ao, ao > 0 ? "бычий" : "медвежий",
                momentum, momentum > 0 ? "бычий" : "медвежий",
                bbPower, bbPower > 0 ? "бычий" : "медвежий",
                currentVolume, volumeRatio, volumeSignal, divergence
        );
    }

    // Moving Averages
    private String calculateMovingAverages(BarSeries series, int lastIndex, double currentPrice) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        double ema10  = new EMAIndicator(close, 10).getValue(lastIndex).doubleValue();
        double ema20  = new EMAIndicator(close, 20).getValue(lastIndex).doubleValue();
        double ema30  = new EMAIndicator(close, 30).getValue(lastIndex).doubleValue();
        double ema50  = new EMAIndicator(close, 50).getValue(lastIndex).doubleValue();
        double ema100 = new EMAIndicator(close, 100).getValue(lastIndex).doubleValue();
        double ema200 = new EMAIndicator(close, 200).getValue(lastIndex).doubleValue();

        double sma10  = new SMAIndicator(close, 10).getValue(lastIndex).doubleValue();
        double sma20  = new SMAIndicator(close, 20).getValue(lastIndex).doubleValue();
        double sma30  = new SMAIndicator(close, 30).getValue(lastIndex).doubleValue();
        double sma50  = new SMAIndicator(close, 50).getValue(lastIndex).doubleValue();
        double sma100 = new SMAIndicator(close, 100).getValue(lastIndex).doubleValue();
        double sma200 = new SMAIndicator(close, 200).getValue(lastIndex).doubleValue();

        // VWMA
        double vwap20 = new VWAPIndicator(series, 20).getValue(lastIndex).doubleValue();

        // Hull MA
        double hma9 = new HMAIndicator(close, 9).getValue(lastIndex).doubleValue();


        // Tenkan-sen (Conversion Line) - период 9
        IchimokuTenkanSenIndicator tenkanSen = new IchimokuTenkanSenIndicator(series, 9);

        // Kijun-sen (Base Line) - период 26
        IchimokuKijunSenIndicator kijunSen = new IchimokuKijunSenIndicator(series, 26);

        // Senkou Span A (Leading Span A)
        IchimokuSenkouSpanAIndicator senkouA = new IchimokuSenkouSpanAIndicator(series, 9, 26);

        // Senkou Span B (Leading Span B) - период 52
        IchimokuSenkouSpanBIndicator senkouB = new IchimokuSenkouSpanBIndicator(series, 52);

        double tenkan = tenkanSen.getValue(lastIndex).doubleValue();
        double kijun  = kijunSen.getValue(lastIndex).doubleValue();
        double spanA  = senkouA.getValue(lastIndex).doubleValue();
        double spanB  = senkouB.getValue(lastIndex).doubleValue();

        return """
            --- Moving Averages ---
            EMA10: %.2f %s | EMA20: %.2f %s | EMA30: %.2f %s
            EMA50: %.2f %s | EMA100: %.2f %s | EMA200: %.2f %s
            SMA10: %.2f %s | SMA20: %.2f %s | SMA30: %.2f %s
            SMA50: %.2f %s | SMA100: %.2f %s | SMA200: %.2f %s
            VWMA(20): %.2f %s
            Hull MA(9): %.2f %s
            Ichimoku:
               Tenkan-sen: %.2f | Kijun-sen: %.2f | Сигнал: %s
               Senkou A: %.2f | Senkou B: %.2f | Облако: %s
            """.formatted(
                ema10,  currentPrice > ema10  ? "▲" : "▼",
                ema20,  currentPrice > ema20  ? "▲" : "▼",
                ema30,  currentPrice > ema30  ? "▲" : "▼",
                ema50,  currentPrice > ema50  ? "▲" : "▼",
                ema100, currentPrice > ema100 ? "▲" : "▼",
                ema200, currentPrice > ema200 ? "▲" : "▼",
                sma10,  currentPrice > sma10  ? "▲" : "▼",
                sma20,  currentPrice > sma20  ? "▲" : "▼",
                sma30,  currentPrice > sma30  ? "▲" : "▼",
                sma50,  currentPrice > sma50  ? "▲" : "▼",
                sma100, currentPrice > sma100 ? "▲" : "▼",
                sma200, currentPrice > sma200 ? "▲" : "▼",
                vwap20, currentPrice > vwap20 ? "▲" : "▼",
                hma9,   currentPrice > hma9   ? "▲" : "▼",
                tenkan, kijun, tenkan > kijun ? "бычий" : "медвежий",
                spanA, spanB, spanA > spanB ? "бычье (зелёное)" : "медвежье (красное)"
        );
    }

    // Pivot Points
    private String calculatePivotPoints(BarSeries series, int lastIndex) {
        Bar prevBar = series.getBar(lastIndex - 1);
        double high  = prevBar.getHighPrice().doubleValue();
        double low   = prevBar.getLowPrice().doubleValue();
        double close = prevBar.getClosePrice().doubleValue();

        // Classic
        double p  = (high + low + close) / 3;
        double r1 = 2 * p - low;
        double r2 = p + (high - low);
        double r3 = high + 2 * (p - low);
        double s1 = 2 * p - high;
        double s2 = p - (high - low);
        double s3 = low - 2 * (high - p);

        // Fibonacci
        double fibR1 = p + 0.382 * (high - low);
        double fibR2 = p + 0.618 * (high - low);
        double fibR3 = p + 1.000 * (high - low);
        double fibS1 = p - 0.382 * (high - low);
        double fibS2 = p - 0.618 * (high - low);
        double fibS3 = p - 1.000 * (high - low);

        return """
            --- Pivot Points ---
            Classic:   R3: %.2f | R2: %.2f | R1: %.2f | P: %.2f | S1: %.2f | S2: %.2f | S3: %.2f
            Fibonacci: R3: %.2f | R2: %.2f | R1: %.2f | P: %.2f | S1: %.2f | S2: %.2f | S3: %.2f
            """.formatted(
                r3, r2, r1, p, s1, s2, s3,
                fibR3, fibR2, fibR1, p, fibS1, fibS2, fibS3
        );
    }

    private String calculateNadarayaWatson(BarSeries series, int lastIndex) {
        int lookback = 100; // сколько свечей анализируем
        double bandwidth = 8.0; // ширина ядра — чем меньше тем чувствительнее
        double multiplier = 3.0; // ширина конверта

        // Собираем цены закрытия
        double[] prices = new double[lookback];
        for (int i = 0; i < lookback; i++) {
            int idx = lastIndex - (lookback - 1 - i);
            prices[i] = series.getBar(idx).getClosePrice().doubleValue();
        }

        // Считаем сглаженные значения через Gaussian kernel
        double[] smoothed = new double[lookback];
        for (int i = 0; i < lookback; i++) {
            double weightedSum = 0;
            double weightSum = 0;
            for (int j = 0; j < lookback; j++) {
                double distance = i - j;
                double weight = Math.exp(-(distance * distance) / (2 * bandwidth * bandwidth));
                weightedSum += weight * prices[j];
                weightSum += weight;
            }
            smoothed[i] = weightedSum / weightSum;
        }

        // Считаем MAE для конверта
        double mae = 0;
        for (int i = 0; i < lookback; i++) {
            mae += Math.abs(prices[i] - smoothed[i]);
        }
        mae /= lookback;

        double currentSmoothed = smoothed[lookback - 1];
        double prevSmoothed    = smoothed[lookback - 2];
        double upper = currentSmoothed + multiplier * mae;
        double lower = currentSmoothed - multiplier * mae;
        double currentPrice = prices[lookback - 1];
        double prevPrice    = prices[lookback - 2];

        // Определяем сигнал
        boolean crossover  = prevPrice <= (prevSmoothed - multiplier * mae)
                && currentPrice > lower; // цена пробила нижнюю границу вверх → BUY
        boolean crossunder = prevPrice >= (prevSmoothed + multiplier * mae)
                && currentPrice < upper; // цена пробила верхнюю границу вниз → SELL

        String signal;
        String position;

        if (crossover)       signal = "🟢 BUY — пробой нижней границы вверх";
        else if (crossunder) signal = "🔴 SELL — пробой верхней границы вниз";
        else                 signal = "⚪ HOLD — цена внутри конверта";

        if (currentPrice > upper)      position = "выше конверта ⚠️";
        else if (currentPrice < lower) position = "ниже конверта ⚠️";
        else                           position = "внутри конверта";

        return """
            --- Nadaraya-Watson Envelope ---
            Сглаженная цена: %.2f
            Верхняя граница: %.2f
            Нижняя граница: %.2f
            Текущая цена: %.2f (%s)
            Сигнал: %s
            """.formatted(
                currentSmoothed, upper, lower, currentPrice, position, signal
        );
    }

    private BarSeries buildSeries(String symbol, String interval) throws Exception {
        String raw = marketInfoService.getKlines(symbol, interval, 300);
        JsonNode list = objectMapper.readTree(raw)
                .path("result")
                .path("list");

        BarSeries series = new BaseBarSeries();

        for (int i = list.size() - 1; i >= 0; i--) {
            JsonNode candle = list.get(i);
            long timestamp  = candle.get(0).asLong();
            double open     = candle.get(1).asDouble();
            double high     = candle.get(2).asDouble();
            double low      = candle.get(3).asDouble();
            double close    = candle.get(4).asDouble();
            double volume   = candle.get(5).asDouble();

            series.addBar(
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()),
                    open, high, low, close, volume
            );
        }
        return series;
    }

    private String rsiSignal(double rsi) {
        if (rsi > 70) return "⚠️ перекуплен";
        if (rsi < 30) return "⚠️ перепродан";
        return "нейтрально";
    }

    private String stochRsiSignal(double stochRsi) {
        if (stochRsi > 0.8) return "⚠️ перекуплен";
        if (stochRsi < 0.2) return "⚠️ перепродан";
        return "нейтрально";
    }
}
