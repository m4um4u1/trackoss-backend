package com.trackoss.trackoss_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackoss.trackoss_backend.dto.AuthResponse;
import com.trackoss.trackoss_backend.dto.LoginRequest;
import com.trackoss.trackoss_backend.dto.RegisterRequest;
import com.trackoss.trackoss_backend.entity.User;
import com.trackoss.trackoss_backend.security.JwtTokenProvider;
import com.trackoss.trackoss_backend.security.UserPrincipal;
import com.trackoss.trackoss_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    private User mockUser;
    private UserPrincipal mockUserPrincipal;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private Authentication mockAuthentication;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockUserPrincipal = new UserPrincipal(mockUser);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");

        mockAuthentication = new UsernamePasswordAuthenticationToken(
                mockUserPrincipal,
                null,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Should authenticate user successfully")
    void testAuthenticateUser_Success() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(mockAuthentication);
        when(tokenProvider.generateToken(any(Authentication.class)))
                .thenReturn("test-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
        verify(tokenProvider, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    @DisplayName("Should return error for invalid credentials")
    void testAuthenticateUser_InvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Invalid username or password!"));

        verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
        verify(tokenProvider, never()).generateToken(any(Authentication.class));
    }

    @Test
    @DisplayName("Should validate login request fields")
    void testAuthenticateUser_ValidationError() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("");
        invalidRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterUser_Success() throws Exception {
        when(userService.existsByUsername(eq("newuser"))).thenReturn(false);
        when(userService.existsByEmail(eq("newuser@example.com"))).thenReturn(false);
        when(userService.createUser(eq("newuser"), eq("newuser@example.com"), eq("password123")))
                .thenReturn(mockUser);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(mockAuthentication);
        when(tokenProvider.generateToken(any(Authentication.class)))
                .thenReturn("test-jwt-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService, times(1)).existsByUsername(eq("newuser"));
        verify(userService, times(1)).existsByEmail(eq("newuser@example.com"));
        verify(userService, times(1)).createUser(eq("newuser"), eq("newuser@example.com"), eq("password123"));
        verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
        verify(tokenProvider, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    @DisplayName("Should return error when username already exists")
    void testRegisterUser_UsernameExists() throws Exception {
        when(userService.existsByUsername(eq("newuser"))).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Username is already taken!"));

        verify(userService, times(1)).existsByUsername(eq("newuser"));
        verify(userService, never()).existsByEmail(anyString());
        verify(userService, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return error when email already exists")
    void testRegisterUser_EmailExists() throws Exception {
        when(userService.existsByUsername(eq("newuser"))).thenReturn(false);
        when(userService.existsByEmail(eq("newuser@example.com"))).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Email is already in use!"));

        verify(userService, times(1)).existsByUsername(eq("newuser"));
        verify(userService, times(1)).existsByEmail(eq("newuser@example.com"));
        verify(userService, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should validate registration request fields")
    void testRegisterUser_ValidationError() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle registration exception")
    void testRegisterUser_ExceptionHandling() throws Exception {
        when(userService.existsByUsername(eq("newuser"))).thenReturn(false);
        when(userService.existsByEmail(eq("newuser@example.com"))).thenReturn(false);
        when(userService.createUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error: Database error")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should get current user successfully")
    void testGetCurrentUser_Success() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                mockUserPrincipal,
                null,
                Collections.emptyList()
        );

        mockMvc.perform(get("/api/auth/me")
                        .principal(auth)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should return error when user not authenticated")
    void testGetCurrentUser_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: User not authenticated!"));
    }

    @Test
    @DisplayName("Should handle null authentication")
    void testGetCurrentUser_NullAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: User not authenticated!"));
    }

    @Test
    @DisplayName("Should handle authentication exception during login")
    void testAuthenticateUser_AuthenticationException() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new RuntimeException("Authentication system error"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Invalid username or password!"));
    }

    @Test
    @DisplayName("Should handle token generation failure")
    void testAuthenticateUser_TokenGenerationFailure() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(mockAuthentication);
        when(tokenProvider.generateToken(any(Authentication.class)))
                .thenThrow(new RuntimeException("Token generation failed"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Invalid username or password!"));
    }
}
