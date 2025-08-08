package com.trackoss.trackoss_backend.security;

import com.trackoss.trackoss_backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private Authentication mockAuthentication;
    private String jwtSecret;
    private final int jwtExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // Generate a valid base64 encoded secret key
        byte[] keyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        jwtSecret = Encoders.BASE64.encode(keyBytes);
        
        // Initialize JwtTokenProvider with test values using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", jwtExpiration);

        User mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create a UserPrincipal wrapper for the mockUser
        UserPrincipal mockUserPrincipal = new UserPrincipal(mockUser);

        mockAuthentication = new UsernamePasswordAuthenticationToken(
                mockUserPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken_Success() {
        String token = jwtTokenProvider.generateToken(mockAuthentication);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // Verify token structure (should have 3 parts separated by dots)
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void testGetUsernameFromToken_Success() {
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateToken_ValidToken() {
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate expired token")
    void testValidateToken_ExpiredToken() {
        // Create a token provider with very short expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(shortLivedProvider, "jwtExpiration", 1); // 1 millisecond

        String token = shortLivedProvider.generateToken(mockAuthentication);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isValid = shortLivedProvider.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate malformed token")
    void testValidateToken_MalformedToken() {
        String malformedToken = "this.is.not.a.valid.jwt.token";
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate token with wrong signature")
    void testValidateToken_WrongSignature() {
        // Create a token with one provider
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        
        // Create a different provider with different secret
        JwtTokenProvider differentProvider = new JwtTokenProvider();
        byte[] differentKeyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        String differentSecret = Encoders.BASE64.encode(differentKeyBytes);
        ReflectionTestUtils.setField(differentProvider, "jwtSecret", differentSecret);
        ReflectionTestUtils.setField(differentProvider, "jwtExpiration", jwtExpiration);
        
        // Try to validate token from first provider using second provider (wrong signature)
        // This should return false or throw an exception - both are valid
        try {
            boolean isValid = differentProvider.validateToken(token);
            assertThat(isValid).isFalse();
        } catch (Exception e) {
            // Exception is also acceptable for wrong signature
            assertThat(e).isInstanceOfAny(SignatureException.class, io.jsonwebtoken.security.SecurityException.class);
        }
    }

    @Test
    @DisplayName("Should handle null token validation")
    void testValidateToken_NullToken() {
        boolean isValid = jwtTokenProvider.validateToken(null);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle empty token validation")
    void testValidateToken_EmptyToken() {
        boolean isValid = jwtTokenProvider.validateToken("");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle whitespace token validation")
    void testValidateToken_WhitespaceToken() {
        boolean isValid = jwtTokenProvider.validateToken("   ");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate token with correct expiration")
    void testGenerateToken_CorrectExpiration() {
        Date beforeGeneration = new Date();
        String token = jwtTokenProvider.generateToken(mockAuthentication);
        Date afterGeneration = new Date();

        // Decode the base64 secret to get the key bytes
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        // Check that issued at is reasonable (within the last minute)
        assertThat(issuedAt).isAfterOrEqualTo(new Date(System.currentTimeMillis() - 60000L));
        assertThat(issuedAt).isBeforeOrEqualTo(new Date());

        // Check that expiration is approximately jwtExpiration after issuedAt
        long expirationDiff = expiration.getTime() - issuedAt.getTime();
        assertThat(expirationDiff).isCloseTo(jwtExpiration, within(5000L)); // within 5 seconds
    }

    @Test
    @DisplayName("Should verify token contains correct subject")
    void testGenerateToken_ContainsSubject() {
        String token = jwtTokenProvider.generateToken(mockAuthentication);

        // Decode the base64 secret to get the key bytes
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // The token should contain the username as subject
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should generate token from username")
    void testGenerateTokenFromUsername() {
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // Should be able to extract username back
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle admin user token generation")
    void testGenerateToken_AdminUser() {
        User adminUser = User.builder()
                .id(2L)
                .username("adminuser")
                .email("admin@example.com")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);
        
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                adminPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        String token = jwtTokenProvider.generateToken(adminAuth);

        // Decode the base64 secret to get the key bytes
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("adminuser");
    }

    @Test
    @DisplayName("Should handle username extraction from invalid token")
    void testGetUsernameFromToken_InvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should handle username extraction from expired token")
    void testGetUsernameFromToken_ExpiredToken() {
        // Create a token provider with very short expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(shortLivedProvider, "jwtExpiration", 1); // 1 millisecond

        String token = shortLivedProvider.generateToken(mockAuthentication);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Even though token is expired, we should still be able to extract username
        // (validation is separate from extraction)
        assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should validate token signed with different secret")
    void testValidateToken_DifferentSecret() {
        // Create another provider with different secret
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        byte[] otherKeyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        String differentSecret = Encoders.BASE64.encode(otherKeyBytes);
        ReflectionTestUtils.setField(otherProvider, "jwtSecret", differentSecret);
        ReflectionTestUtils.setField(otherProvider, "jwtExpiration", jwtExpiration);

        // Generate token with first provider
        String token = jwtTokenProvider.generateToken(mockAuthentication);

        // Try to validate with second provider (different secret)
        try {
            boolean isValid = otherProvider.validateToken(token);
            assertThat(isValid).isFalse();
        } catch (Exception e) {
            // Also acceptable if validation throws an exception
            assertThat(e).isInstanceOf(io.jsonwebtoken.security.SecurityException.class);
        }
    }

    @Test
    @DisplayName("Should handle null authentication in token generation")
    void testGenerateToken_NullAuthentication() {
        assertThatThrownBy(() -> jwtTokenProvider.generateToken(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle authentication without User principal")
    void testGenerateToken_NonUserPrincipal() {
        Authentication authWithString = new UsernamePasswordAuthenticationToken(
                "simpleUsername",
                null,
                Collections.emptyList()
        );

        assertThatThrownBy(() -> jwtTokenProvider.generateToken(authWithString))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateToken_DifferentUsers() {
        String token1 = jwtTokenProvider.generateToken(mockAuthentication);

        User anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .email("another@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserPrincipal anotherPrincipal = new UserPrincipal(anotherUser);
        
        Authentication anotherAuth = new UsernamePasswordAuthenticationToken(
                anotherPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token2 = jwtTokenProvider.generateToken(anotherAuth);

        assertThat(token1).isNotEqualTo(token2);
        
        assertThat(jwtTokenProvider.getUsernameFromToken(token1)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.getUsernameFromToken(token2)).isEqualTo("anotheruser");
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testGenerateToken_DifferentTimestamps() throws InterruptedException {
        String token1 = jwtTokenProvider.generateToken(mockAuthentication);
        Thread.sleep(1000); // Longer delay to ensure different timestamp
        String token2 = jwtTokenProvider.generateToken(mockAuthentication);

        // Extract claims to verify issuedAt timestamps are different
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        Claims claims1 = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token1)
                .getPayload();
                
        Claims claims2 = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token2)
                .getPayload();
        
        // Verify issuedAt times are different
        assertThat(claims1.getIssuedAt()).isNotEqualTo(claims2.getIssuedAt());
        
        // Both should have same username
        assertThat(claims1.getSubject()).isEqualTo("testuser");
        assertThat(claims2.getSubject()).isEqualTo("testuser");
    }
}
