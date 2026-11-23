package com.wallet.system.service;

import com.wallet.system.dto.request.RegisterRequest;
import com.wallet.system.dto.response.AuthResponse;
import com.wallet.system.entity.User;
import com.wallet.system.entity.Wallet;
import com.wallet.system.enums.Role;
import com.wallet.system.exception.DuplicateResourceException;
import com.wallet.system.repository.UserRepository;
import com.wallet.system.repository.WalletRepository;
import com.wallet.system.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    private User user;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("Password@123");

        user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
    }


    @Test
    void register_shouldRegisterUserSuccessfully() {

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        authService.register(registerRequest);

        verify(userRepository).existsByEmail("test@gmail.com");

        verify(passwordEncoder).encode("Password@123");

        verify(userRepository).save(any(User.class));

        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void register_shouldNormalizeEmail() {

        registerRequest.setEmail("  TEST@GMAIL.COM  ");

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registerRequest);

        verify(userRepository).existsByEmail("test@gmail.com");

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getEmail().equals("test@gmail.com")
        ));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(registerRequest)
        );

        verify(userRepository, never()).save(any(User.class));

        verify(walletRepository, never()).save(any(Wallet.class));

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_shouldCreateWalletWithZeroBalance() {

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        authService.register(registerRequest);

        verify(walletRepository).save(argThat(wallet ->
                wallet.getUser().getId().equals(1L)
                        && wallet.getBalance().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    void register_shouldAssignUserRole() {

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registerRequest);

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getRole() == Role.USER
        ));
    }


    @Test
    void login_shouldReturnJwtTokenSuccessfully() {

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(
                "test@gmail.com",
                "Password@123"
        );

        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());

        verify(authenticationManager).authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ));

        verify(jwtService).generateToken(any(UserDetails.class));
    }

    @Test
    void login_shouldNormalizeEmail() {

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn("mock-jwt-token");

        authService.login(
                "  TEST@GMAIL.COM  ",
                "Password@123"
        );

        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    void login_shouldThrowWhenAuthenticatedUserNotFound() {

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> authService.login(
                        "test@gmail.com",
                        "Password@123"
                )
        );

        verify(jwtService, never())
                .generateToken(any(UserDetails.class));
    }
}