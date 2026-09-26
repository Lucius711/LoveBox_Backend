package com.lovebox.modules.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private String geminiApiKey;
    private String chatModel = "gemini-2.0-flash";
    private String fallbackChatModel = "gemini-1.5-flash";
}
