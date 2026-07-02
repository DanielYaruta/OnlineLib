package com.example.onlinelib.service;

import com.example.onlinelib.dto.RegisterRequest;
import com.example.onlinelib.dto.UserResponse;
import com.example.onlinelib.entity.User;
import com.example.onlinelib.exception.UsernameAlreadyExistsException;
import com.example.onlinelib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken"
            );
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRole());
    }
}
