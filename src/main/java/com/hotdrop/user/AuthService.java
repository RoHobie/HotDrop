package com.hotdrop.user;

import com.hotdrop.security.JwtService;
import com.hotdrop.user.dto.AuthResponse;
import com.hotdrop.user.dto.LoginRequest;
import com.hotdrop.user.dto.SignupRequest;
import com.hotdrop.user.dto.UserDto;
import com.hotdrop.user.exception.UserAlreadyExistsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserDto signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already registered: " + request.email());
        }

        Role role = request.role() != null ? request.role() : Role.USER;
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.name(), request.email(), encodedPassword, role);
        User saved = userRepository.save(user);

        return UserDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserDto.fromEntity(user));
    }
}
