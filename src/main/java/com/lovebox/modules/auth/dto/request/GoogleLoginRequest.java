package com.lovebox.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token không được để trống")
    private String idToken;
}
