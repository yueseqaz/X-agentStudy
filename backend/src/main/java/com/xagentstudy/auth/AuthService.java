package com.xagentstudy.auth;

import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private static final String DEMO_ACCOUNT = "demo@xagentstudy.local";
    private static final String DEMO_PASSWORD = "demo123456";

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountActionTokenRepository actionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AccountActionTokenRepository actionTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.actionTokenRepository = actionTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String account = request.account().trim().toLowerCase();
        if (userRepository.existsByAccount(account)) {
            throw new BusinessException("ACCOUNT_EXISTS", "Account already exists");
        }
        String role = userRepository.count() == 0 ? "ADMIN" : "USER";
        AppUser saved = userRepository.save(new AppUser(
                request.nickname().trim(),
                account,
                passwordEncoder.encode(request.password()),
                role
        ));
        return issue(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String account = request.account().trim().toLowerCase();
        AppUser user = userRepository.findByAccount(account)
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid account or password"));
        user.clearExpiredBan(OffsetDateTime.now());
        if (user.isBanActive(OffsetDateTime.now())) {
            String until = user.getDisabledUntil() == null ? "永久" : user.getDisabledUntil().toString();
            throw new BusinessException("ACCOUNT_DISABLED", "账号已被封禁，封禁截止时间：" + until);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException("ACCOUNT_LOCKED", "Too many failed login attempts");
        }

        if (user.getPasswordHash() == null && DEMO_ACCOUNT.equals(account) && DEMO_PASSWORD.equals(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordLoginFailure();
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid account or password");
        }
        user.recordLoginSuccess();
        return issue(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
                .filter(item -> !Boolean.TRUE.equals(item.getRevoked()))
                .filter(item -> item.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        token.revoke();
        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        return issue(user);
    }

    @Transactional
    public AccountActionTokenResponse requestPasswordReset(PasswordResetRequest request) {
        AppUser user = userRepository.findByAccount(request.account().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Account not found"));
        String token = UUID.randomUUID().toString().replace("-", "");
        actionTokenRepository.save(new AccountActionToken(user.getId(), hash(token), "PASSWORD_RESET", OffsetDateTime.now().plusHours(1)));
        return new AccountActionTokenResponse("Password reset token generated for local development", token);
    }

    @Transactional
    public UserSession confirmPasswordReset(PasswordResetConfirmRequest request) {
        AccountActionToken token = actionTokenRepository.findByTokenHashAndActionType(hash(request.token()), "PASSWORD_RESET")
                .filter(item -> !Boolean.TRUE.equals(item.getUsed()))
                .filter(item -> item.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid token"));
        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.markUsed();
        return UserSession.from(user);
    }

    @Transactional
    public AccountActionTokenResponse requestEmailVerification() {
        AppUser user = userRepository.findById(AuthContext.currentUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        String token = UUID.randomUUID().toString().replace("-", "");
        actionTokenRepository.save(new AccountActionToken(user.getId(), hash(token), "EMAIL_VERIFY", OffsetDateTime.now().plusHours(24)));
        return new AccountActionTokenResponse("Email verification token generated for local development", token);
    }

    @Transactional
    public UserSession verifyEmail(RefreshTokenRequest request) {
        AccountActionToken token = actionTokenRepository.findByTokenHashAndActionType(hash(request.refreshToken()), "EMAIL_VERIFY")
                .filter(item -> !Boolean.TRUE.equals(item.getUsed()))
                .filter(item -> item.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid token"));
        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        user.verifyEmail();
        token.markUsed();
        return UserSession.from(user);
    }

    @Transactional(readOnly = true)
    public UserSession me() {
        AppUser user = userRepository.findById(AuthContext.currentUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        return UserSession.from(user);
    }

    @Transactional(readOnly = true)
    public SecurityStatusResponse securityStatus() {
        AppUser user = userRepository.findById(AuthContext.currentUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        return SecurityStatusResponse.from(user, refreshTokenRepository.existsByUserIdAndRevokedFalse(user.getId()));
    }

    @Transactional
    public UserSession uploadAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "Avatar file is empty");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!List.of("image/png", "image/jpeg", "image/webp", "image/gif").contains(contentType)) {
            throw new BusinessException("UNSUPPORTED_AVATAR_TYPE", "Only png/jpeg/webp/gif avatars are supported");
        }
        if (file.getSize() > 512 * 1024) {
            throw new BusinessException("AVATAR_TOO_LARGE", "Avatar must be 512KB or smaller");
        }
        AppUser user = userRepository.findById(AuthContext.currentUserId())
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
        try {
            String encoded = Base64.getEncoder().encodeToString(file.getBytes());
            user.setAvatarUrl("data:" + contentType + ";base64," + encoded);
            return UserSession.from(user);
        } catch (IOException ex) {
            throw new BusinessException("AVATAR_UPLOAD_FAILED", "Failed to read avatar file");
        }
    }

    private AuthResponse issue(AppUser user) {
        String token = jwtService.issue(new AuthPrincipal(user.getId(), user.getAccount(), user.getRole()));
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        refreshTokenRepository.save(new RefreshToken(user.getId(), hash(refreshToken), OffsetDateTime.now().plusDays(30)));
        return new AuthResponse(token, refreshToken, UserSession.from(user));
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}
