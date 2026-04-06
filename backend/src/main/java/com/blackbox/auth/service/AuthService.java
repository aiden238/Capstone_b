package com.blackbox.auth.service;

import com.blackbox.auth.dto.*;
import com.blackbox.auth.entity.User;
import com.blackbox.auth.entity.UserRole;
import com.blackbox.auth.jwt.JwtTokenProvider;
import com.blackbox.auth.jwt.RefreshTokenService;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        UserRole role = UserRole.STUDENT;
        if (request.getRole() != null) {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                role = UserRole.STUDENT;
            }
        }

        User user = new User(
                request.getEmail(),
                request.getName(),
                passwordEncoder.encode(request.getPassword()),
                role
        );

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenService.save(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpiration());

        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        String storedToken = refreshTokenService.find(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenService.save(user.getId(), newRefreshToken, jwtTokenProvider.getRefreshTokenExpiration());

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public void logout(UUID userId, String accessToken) {
        refreshTokenService.delete(userId);
        if (accessToken != null) {
            long remaining = jwtTokenProvider.getRemainingExpiration(accessToken);
            refreshTokenService.blacklistAccessToken(accessToken, remaining);
        }
    }

    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }
}
