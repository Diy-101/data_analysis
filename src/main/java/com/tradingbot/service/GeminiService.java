package com.tradingbot.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;

    public GeminiService(@Value("${gemini.api-key}") String apiKey) {
        this.client = Client.builder().apiKey(apiKey).build();
    }

    public String generateAIResponse(String promt) {
        GenerateContentResponse AIResponse =
                client.models.generateContent(
                        "gemini-3-flash-preview",
                        promt,
                        null
                );
        return AIResponse.text();
    }
}
