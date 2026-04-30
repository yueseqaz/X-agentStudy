package com.xagentstudy.auth;

import com.xagentstudy.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<AccountActionTokenResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ApiResponse.ok(authService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<UserSession> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return ApiResponse.ok(authService.confirmPasswordReset(request));
    }

    @PostMapping("/email-verification/request")
    public ApiResponse<AccountActionTokenResponse> requestEmailVerification() {
        return ApiResponse.ok(authService.requestEmailVerification());
    }

    @PostMapping("/email-verification/confirm")
    public ApiResponse<UserSession> verifyEmail(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.verifyEmail(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserSession> me() {
        return ApiResponse.ok(authService.me());
    }

    @GetMapping("/security")
    public ApiResponse<SecurityStatusResponse> security() {
        return ApiResponse.ok(authService.securityStatus());
    }

    @PostMapping("/avatar")
    public ApiResponse<UserSession> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(authService.uploadAvatar(file));
    }
}
