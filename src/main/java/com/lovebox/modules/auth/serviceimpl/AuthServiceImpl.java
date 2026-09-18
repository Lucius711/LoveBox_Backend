package com.lovebox.modules.auth.serviceimpl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.common.util.HashUtil;
import com.lovebox.modules.auth.dto.request.GoogleLoginRequest;
import com.lovebox.modules.auth.dto.request.RefreshTokenRequest;
import com.lovebox.modules.auth.dto.response.TokenResponse;
import com.lovebox.modules.auth.entity.AuthSession;
import com.lovebox.modules.auth.repository.AuthSessionRepository;
import com.lovebox.modules.auth.service.AuthService;
import com.lovebox.modules.user.dto.response.UserResponse;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.repository.UserRepository;
import com.lovebox.security.GoogleTokenVerifier;
import com.lovebox.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
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

        return issueTokens(user, httpRequest);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = HashUtil.sha256(request.getRefreshToken());
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

    @Override
    @Transactional
    public void logout(UUID userId) {
        authSessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    private TokenResponse issueTokens(User user, HttpServletRequest httpRequest) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        String refreshTokenHash = HashUtil.sha256(refreshToken);

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
                .accessTokenExpiresIn(refreshTokenExpirationMs / 1000)
                .user(UserResponse.from(user))
                .build();
    }
}
