package com.blackbox.auth.security;

import com.blackbox.auth.entity.User;
import com.blackbox.auth.entity.UserRole;
import com.blackbox.auth.jwt.JwtTokenProvider;
import com.blackbox.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.blackbox.auth.jwt.RefreshTokenService;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository,
                                   RefreshTokenService refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token) && !refreshTokenService.isBlacklisted(token)) {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            String roleStr = jwtTokenProvider.getRoleFromToken(token);

            CustomUserDetails userDetails = new CustomUserDetails(
                    userId,
                    null,
                    UserRole.valueOf(roleStr)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
