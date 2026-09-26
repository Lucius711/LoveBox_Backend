package com.lovebox.modules.auth.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.auth.dto.request.GoogleLoginRequest;
import com.lovebox.modules.auth.dto.request.RefreshTokenRequest;
import com.lovebox.modules.auth.dto.response.TokenResponse;
import com.lovebox.modules.auth.service.AuthService;
import com.lovebox.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url}") private String frontendUrl;

    private static final String STATE_COOKIE = "g_oauth_state";

    /** Bước 1: FE chuyển trình duyệt tới đây → backend redirect sang trang chọn tài khoản Google. */
    @GetMapping("/google/login")
    public void googleLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String state = UUID.randomUUID().toString();   // chống CSRF: so khớp lại ở callback
        res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(STATE_COOKIE, state)
                .httpOnly(true).secure(req.isSecure()).sameSite("Lax")
                .path(req.getContextPath() + "/auth/google").maxAge(Duration.ofMinutes(10)).build().toString());
        res.sendRedirect(authService.googleAuthorizeUrl(state));
    }

    /**
     * Bước 2: Google gọi về (Authorized redirect URI). Đổi code → đăng nhập → trả token cho FE qua #fragment
     * (fragment không gửi lên server / không lọt vào log).
     */
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code, @RequestParam(required = false) String state,
                               @CookieValue(name = STATE_COOKIE, required = false) String expected,
                               HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(STATE_COOKIE, "")
                .path(req.getContextPath() + "/auth/google").maxAge(0).build().toString());
        if (code == null || state == null || !state.equals(expected)) {
            res.sendRedirect(frontendUrl + "/login?error=google");
            return;
        }
        try {
            TokenResponse t = authService.loginWithGoogleCode(code, req);
            res.sendRedirect(frontendUrl + "/auth/callback#accessToken=" + enc(t.getAccessToken())
                    + "&refreshToken=" + enc(t.getRefreshToken()));
        } catch (Exception e) {
            res.sendRedirect(frontendUrl + "/login?error=google");
        }
    }

    private static String enc(String s) { return UriUtils.encode(s, StandardCharsets.UTF_8); }

    @PostMapping("/google")
    public ApiResponse<TokenResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.loginWithGoogle(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ApiResponse.success("Đăng xuất thành công", null);
    }
}
