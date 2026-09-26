package com.lovebox.modules.auth.dto.response;

import com.lovebox.modules.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}
