package com.example.onlinelib.service;

import com.example.onlinelib.dto.RegisterRequest;
import com.example.onlinelib.entity.User;
import com.example.onlinelib.exception.UsernameAlreadyExistsException;
import com.example.onlinelib.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = new RegisterRequest("existingUser", "password123");

        when(userRepository.findByUsername("existingUser"))
                .thenReturn(Optional.of(User.builder().username("existingUser").build()));

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any());
    }
}
