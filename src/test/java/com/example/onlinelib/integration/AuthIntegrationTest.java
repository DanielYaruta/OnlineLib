package com.example.onlinelib.integration;

import com.example.onlinelib.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.findByUsername("reader1").ifPresent(userRepository::delete);
    }

    @Test
    void fullScenario() throws Exception {
        // 1. /user/hello без авторизации → 401
        mockMvc.perform(get("/user/hello"))
                .andExpect(status().isUnauthorized());

        // 2. Регистрация нового пользователя → 201
        String registerJson = """
                {"username": "reader1", "password": "password123"}
                """;

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("reader1"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.id").isNumber());

        // 3. Зарегистрированный пользователь → /user/hello → 200
        mockMvc.perform(get("/user/hello")
                        .with(httpBasic("reader1", "password123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Привет, читатель!"));

        // 4. Обычный пользователь → /admin/hello → 403
        mockMvc.perform(get("/admin/hello")
                        .with(httpBasic("reader1", "password123")))
                .andExpect(status().isForbidden());

        // 5. admin/admin → /admin/hello → 200
        mockMvc.perform(get("/admin/hello")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("Привет, библиотекарь!"));
    }
}
