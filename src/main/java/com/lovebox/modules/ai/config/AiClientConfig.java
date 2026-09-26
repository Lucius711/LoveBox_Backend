package com.lovebox.modules.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    /** Gemini qua endpoint OpenAI-compatible — dùng để bóc tách từ khoá. */
    @Bean
    public RestClient geminiChatClient(AiProperties props) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5_000);
        rf.setReadTimeout(15_000);
        return RestClient.builder()
                .requestFactory(rf)
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
                .defaultHeader("Authorization", "Bearer " + props.getGeminiApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
