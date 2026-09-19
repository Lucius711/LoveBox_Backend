package com.lovebox.modules.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter @Builder
public class ChatPreferencesResponse {
    private UUID id;
    private String preferredLanguage;
    private List<String> stylePreferences;
    private List<String> occasionInterests;
    private String budgetRange;
}
