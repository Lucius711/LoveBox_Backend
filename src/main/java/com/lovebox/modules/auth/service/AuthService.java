package com.lovebox.modules.auth.service;

import com.lovebox.modules.auth.dto.request.GoogleLoginRequest;
import com.lovebox.modules.auth.dto.request.RefreshTokenRequest;
import com.lovebox.modules.auth.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface AuthService {
    TokenResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void logout(UUID userId);
}
