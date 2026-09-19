package com.lovebox.modules.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class UpdatePreferencesRequest {
    private String preferredLanguage;
    private List<String> stylePreferences;
    private List<String> occasionInterests;
    private String budgetRange;
}
