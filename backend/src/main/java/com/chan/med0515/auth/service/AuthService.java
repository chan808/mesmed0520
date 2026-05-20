package com.chan.med0515.auth.service;

import com.chan.med0515.auth.AuthErrorCode;
import com.chan.med0515.auth.dto.LoginRequest;
import com.chan.med0515.auth.dto.LoginResponse;
import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.global.security.JwtProvider;
import com.chan.med0515.user.entity.User;
import com.chan.med0515.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 사용자 아이디와 비밀번호를 검증하고, 성공 시 JWT 토큰을 발급하여 로그인 응답을 반환합니다.
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtProvider.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getRole().name());
    }
}