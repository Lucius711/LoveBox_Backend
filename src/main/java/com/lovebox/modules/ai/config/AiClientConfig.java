package com.lovebox.modules.ai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    /** Gemini qua OpenAI-compatible endpoint — dùng cho chat completion */
    @Bean
    @Qualifier("geminiChatClient")
    public RestClient geminiChatClient(AiProperties props) {
        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
                .defaultHeader("Authorization", "Bearer " + props.getGeminiApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /** Gemini native endpoint — dùng cho Imagen 3 */
    @Bean
    @Qualifier("geminiImageClient")
    public RestClient geminiImageClient(AiProperties props) {
        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
