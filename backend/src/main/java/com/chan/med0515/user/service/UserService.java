package com.chan.med0515.user.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.user.UserErrorCode;
import com.chan.med0515.user.dto.UserRequest;
import com.chan.med0515.user.dto.UserResponse;
import com.chan.med0515.user.entity.User;
import com.chan.med0515.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 신규 사용자를 등록하며, 아이디 중복 확인 및 비밀번호 암호화를 수행합니다.
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException (UserErrorCode.USERNAME_DUPLICATED);
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(request.role())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUser(Long id) {
        return UserResponse.from(findEntityById(id));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
