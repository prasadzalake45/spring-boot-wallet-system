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
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }


    @Transactional
    public void register(RegisterRequest request){
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);
        User savedUser = userRepository.save(user);

        // Automatically create wallet for this user
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
    }


    public AuthResponse login(String email, String password) {

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        password
                )
        );

        UserDetails userDetails;
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );
        userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, "Bearer");
    }
}
