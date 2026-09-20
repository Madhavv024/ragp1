package com.madhavv.enterpriserag.service;

import com.madhavv.enterpriserag.Exception.InvalidCredentialsException;
import com.madhavv.enterpriserag.dto.LoginRequest;
import com.madhavv.enterpriserag.dto.RegisterRequest;
import com.madhavv.enterpriserag.dto.UserResponse;
import com.madhavv.enterpriserag.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UUID userId = UUID.randomUUID();

        String passwordHash = passwordEncoder.encode(request.password());

        userRepository.save(
                userId,
                email,
                passwordHash
        );

        return new UserResponse(userId, email);
    }

    public LoginResult login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        UserRepository.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(
                user.id(),
                user.email()
        );

        return new LoginResult(
                token,
                new UserResponse(
                        user.id(),
                        user.email()
                )
        );
    }

    public record LoginResult(
            String token,
            UserResponse user
    ) {}
}