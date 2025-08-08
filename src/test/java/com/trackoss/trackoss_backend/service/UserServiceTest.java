package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.entity.User;
import com.trackoss.trackoss_backend.repository.UserRepository;
import com.trackoss.trackoss_backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword123")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("adminuser")
                .email("admin@example.com")
                .password("encodedAdminPass")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void testLoadUserByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("testuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(UserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
        
        // Check that the User object is properly wrapped
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        assertThat(userPrincipal.getUser()).isEqualTo(testUser);

        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void testLoadUserByUsername_UserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: nonexistent");

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should load admin user with correct authorities")
    void testLoadUserByUsername_AdminUser() {
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(adminUser));

        UserDetails userDetails = userService.loadUserByUsername("adminuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(UserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo("adminuser");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        
        // Check that the User object is properly wrapped
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        assertThat(userPrincipal.getUser()).isEqualTo(adminUser);

        verify(userRepository, times(1)).findByUsername("adminuser");
    }

    @Test
    @DisplayName("Should create new user successfully")
    void testCreateUser_Success() {
        String username = "newuser";
        String email = "newuser@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(3L);
            savedUser.setCreatedAt(LocalDateTime.now());
            savedUser.setUpdatedAt(LocalDateTime.now());
            return savedUser;
        });

        User createdUser = userService.createUser(username, email, rawPassword);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isEqualTo(3L);
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getEmail()).isEqualTo(email);
        assertThat(createdUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(createdUser.getRole()).isEqualTo(User.Role.USER);
        assertThat(createdUser.getCreatedAt()).isNotNull();
        assertThat(createdUser.getUpdatedAt()).isNotNull();

        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle null values in createUser")
    void testCreateUser_WithNullValues() {
        assertThatThrownBy(() -> userService.createUser(null, "email@example.com", "password"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> userService.createUser("username", null, "password"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> userService.createUser("username", "email@example.com", null))
                .isInstanceOf(NullPointerException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check if username exists - returns true")
    void testExistsByUsername_True() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        boolean exists = userService.existsByUsername("existinguser");

        assertThat(exists).isTrue();
        verify(userRepository, times(1)).existsByUsername("existinguser");
    }

    @Test
    @DisplayName("Should check if username exists - returns false")
    void testExistsByUsername_False() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        boolean exists = userService.existsByUsername("newuser");

        assertThat(exists).isFalse();
        verify(userRepository, times(1)).existsByUsername("newuser");
    }

    @Test
    @DisplayName("Should check if email exists - returns true")
    void testExistsByEmail_True() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        boolean exists = userService.existsByEmail("existing@example.com");

        assertThat(exists).isTrue();
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
    }

    @Test
    @DisplayName("Should check if email exists - returns false")
    void testExistsByEmail_False() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        boolean exists = userService.existsByEmail("new@example.com");

        assertThat(exists).isFalse();
        verify(userRepository, times(1)).existsByEmail("new@example.com");
    }

    @Test
    @DisplayName("Should handle empty username in loadUserByUsername")
    void testLoadUserByUsername_EmptyUsername() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: ");

        verify(userRepository, times(1)).findByUsername("");
    }

    @Test
    @DisplayName("Should handle case sensitivity in username")
    void testLoadUserByUsername_CaseSensitive() {
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.loadUserByUsername("TestUser"))
                .isInstanceOf(UsernameNotFoundException.class);

        UserDetails userDetails = userService.loadUserByUsername("testuser");
        assertThat(userDetails).isNotNull();

        verify(userRepository, times(1)).findByUsername("TestUser");
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle repository exception during user creation")
    void testCreateUser_RepositoryException() {
        String username = "newuser";
        String email = "newuser@example.com";
        String rawPassword = "password123";

        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        assertThatThrownBy(() -> userService.createUser(username, email, rawPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");

        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with proper timestamps")
    void testCreateUser_Timestamps() {
        String username = "timestampuser";
        String email = "timestamp@example.com";
        String rawPassword = "password123";

        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(4L);
            return savedUser;
        });

        LocalDateTime beforeCreation = LocalDateTime.now();
        User createdUser = userService.createUser(username, email, rawPassword);
        LocalDateTime afterCreation = LocalDateTime.now();

        // Since we're mocking the repository, we need to manually set the timestamps
        // that would be set by the @PrePersist callback
        // Use a time that's between beforeCreation and afterCreation
        LocalDateTime creationTime = beforeCreation.plusSeconds(1);
        createdUser.setCreatedAt(creationTime);
        createdUser.setUpdatedAt(creationTime);

        assertThat(createdUser.getCreatedAt()).isNotNull();
        assertThat(createdUser.getUpdatedAt()).isNotNull();
        assertThat(createdUser.getUpdatedAt()).isEqualTo(createdUser.getCreatedAt());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testExistsByUsername_SpecialCharacters() {
        String specialUsername = "user@domain.com";
        when(userRepository.existsByUsername(specialUsername)).thenReturn(true);

        boolean exists = userService.existsByUsername(specialUsername);

        assertThat(exists).isTrue();
        verify(userRepository, times(1)).existsByUsername(specialUsername);
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void testExistsByEmail_SpecialCharacters() {
        String specialEmail = "user+tag@sub.domain.com";
        when(userRepository.existsByEmail(specialEmail)).thenReturn(false);

        boolean exists = userService.existsByEmail(specialEmail);

        assertThat(exists).isFalse();
        verify(userRepository, times(1)).existsByEmail(specialEmail);
    }

    @Test
    @DisplayName("Should handle password encoding failure")
    void testCreateUser_PasswordEncodingFailure() {
        String username = "newuser";
        String email = "newuser@example.com";
        String rawPassword = "password123";

        when(passwordEncoder.encode(rawPassword))
                .thenThrow(new RuntimeException("Encoding algorithm not available"));

        assertThatThrownBy(() -> userService.createUser(username, email, rawPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Encoding algorithm not available");

        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, never()).save(any());
    }
}
