package com.lovebox.modules.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.lovebox.modules.auth.dto.request.GoogleLoginRequest;
import com.lovebox.modules.auth.dto.request.RefreshTokenRequest;
import com.lovebox.modules.auth.dto.response.TokenResponse;
import com.lovebox.modules.auth.entity.AuthSession;
import com.lovebox.modules.auth.repository.AuthSessionRepository;
import com.lovebox.modules.user.dto.response.UserResponse;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.repository.UserRepository;
import com.lovebox.security.GoogleTokenVerifier;
import com.lovebox.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    /** Email trong danh sách này tự động có quyền ADMIN khi đăng nhập (ADMIN_EMAILS, cách nhau bởi dấu phẩy). */
    @Value("${app.admin-emails:}")
    private java.util.List<String> adminEmails;


    @Value("${app.google.client-id}") private String googleClientId;
    @Value("${app.google.client-secret:}") private String googleClientSecret;
    @Value("${app.google.redirect-uri:}") private String googleRedirectUri;
    private final RestClient googleOAuth = RestClient.create("https://oauth2.googleapis.com");

    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        return loginWithIdToken(request.getIdToken(), httpRequest);
    }

    /** Luồng redirect: link đăng nhập Google của backend (redirect_uri = callback). */
    public String googleAuthorizeUrl(String state) {
        return org.springframework.web.util.UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("prompt", "select_account")
                .queryParam("state", state)
                .encode().build().toUriString();
    }

    /** Callback: đổi authorization code lấy id_token (server-to-server, có client secret) rồi đăng nhập. */
    @Transactional
    public TokenResponse loginWithGoogleCode(String code, HttpServletRequest httpRequest) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", googleRedirectUri);
        form.add("grant_type", "authorization_code");
        java.util.Map<?, ?> res;
        try {
            res = googleOAuth.post().uri("/token").contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(java.util.Map.class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
        Object idToken = res == null ? null : res.get("id_token");
        if (idToken == null) throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        return loginWithIdToken(idToken.toString(), httpRequest);
    }

    private TokenResponse loginWithIdToken(String idToken, HttpServletRequest httpRequest) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);
        if (payload == null) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");

        User user = userRepository.findByGoogleSub(googleSub)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setName(name != null ? name : existing.getName());
                    existing.setAvatarUrl(avatarUrl);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleSub(googleSub)
                        .email(email)
                        .name(name != null ? name : email)
                        .avatarUrl(avatarUrl)
                        .build()));
        if (adminEmails.stream().anyMatch(e -> e.trim().equalsIgnoreCase(email))) user.setRole(User.ADMIN);

        return issueTokens(user, httpRequest);
    }


    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = sha256(request.getRefreshToken());
        AuthSession session = authSessionRepository
                .findByRefreshTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setRevokedAt(Instant.now());
            authSessionRepository.save(session);
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        session.setRevokedAt(Instant.now());
        authSessionRepository.save(session);

        return issueTokens(user, null);
    }


    @Transactional
    public void logout(UUID userId) {
        authSessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    private TokenResponse issueTokens(User user, HttpServletRequest httpRequest) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        String refreshTokenHash = sha256(refreshToken);

        AuthSession session = AuthSession.builder()
                .userId(user.getId())
                .refreshTokenHash(refreshTokenHash)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .userAgent(httpRequest != null ? httpRequest.getHeader("User-Agent") : null)
                .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : null)
                .build();
        authSessionRepository.save(session);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.from(user))
                .build();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
