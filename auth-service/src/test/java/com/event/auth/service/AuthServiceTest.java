package com.event.auth.service;

import com.event.auth.dto.LoginRequest;
import com.event.auth.dto.LoginResponse;
import com.event.auth.dto.UserRequest;
import com.event.auth.dto.UserResponse;
import com.event.auth.entity.User;
import com.event.auth.repository.UserProfileRepository;
import com.event.auth.repository.UserRepository;
import com.event.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    // ─────────────────────────────────────────────────────────────
    // 1. registerUser_Success
    // ─────────────────────────────────────────────────────────────
    @Test
    public void registerUser_Success() {
        UserRequest request = new UserRequest();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("ROLE_REGISTRANT");
        request.setFullName("John Doe");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pwd");

        User savedUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .role("ROLE_REGISTRANT")
                .isActive(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.createUser(request);

        assertNotNull(response);
        assertEquals("johndoe", response.getUsername());
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // 2. registerUser_EmailAlreadyExists_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void registerUser_EmailAlreadyExists_ThrowsException() {
        UserRequest request = new UserRequest();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new User()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.createUser(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 3. registerUser_UsernameAlreadyExists_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void registerUser_UsernameAlreadyExists_ThrowsException() {
        UserRequest request = new UserRequest();
        request.setUsername("johndoe");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(new User()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.createUser(request);
        });

        assertEquals("Username already exists", exception.getMessage());
    }

    // ─────────────────────────────────────────────────────────────
    // 4. login_Success
    // ─────────────────────────────────────────────────────────────
    @Test
    public void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("johndoe");
        request.setPassword("password123");

        UserDetails mockUserDetails = mock(UserDetails.class);
        User mockUser = User.builder().id(1L).username("johndoe").email("john@example.com").role("ROLE_REGISTRANT").build();

        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(mockUserDetails);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(mockUserDetails, 1L, "john@example.com")).thenReturn("mocked-jwt-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals("johndoe", response.getUsername());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 5. login_BadCredentials_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void login_BadCredentials_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("johndoe");
        request.setPassword("wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 6. getUsersByRole_Success
    // ─────────────────────────────────────────────────────────────
    @Test
    public void getUsersByRole_Success() {
        User user1 = User.builder().id(1L).username("admin1").role("ROLE_ADMIN").build();
        User user2 = User.builder().id(2L).username("admin2").role("ROLE_ADMIN").build();

        when(userRepository.findByRole("ROLE_ADMIN")).thenReturn(java.util.Arrays.asList(user1, user2));

        java.util.List<UserResponse> responses = authService.getUsersByRole("ROLE_ADMIN");

        assertEquals(2, responses.size());
        assertEquals("admin1", responses.get(0).getUsername());
        assertEquals("admin2", responses.get(1).getUsername());
    }
}
