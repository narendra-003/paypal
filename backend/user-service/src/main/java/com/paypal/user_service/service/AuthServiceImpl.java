package com.paypal.user_service.service;

import com.paypal.user_service.dto.JwtAuthResponse;
import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.exception.ApiException;
import com.paypal.user_service.exception.EmailAlreadyExistException;
import com.paypal.user_service.exception.UserNotFoundException;
import com.paypal.user_service.repository.UserRepository;
import com.paypal.user_service.security.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public JwtAuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = null;
        try {
            logger.debug("Attempting authentication for user: {}", loginRequest.getEmail());
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            logger.info("Authentication successful for user: {}", loginRequest.getEmail());
        } catch (BadCredentialsException ex) {
            logger.warn("Failed authentication attempt for user: {}", loginRequest.getEmail());
            throw new ApiException("Invalid Username or Password !!!");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername();

        User savedUser = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole());

        logger.debug("Generating JWT token for user: {}", userEmail);
        String token = jwtUtil.generateToken(claims, userEmail);
        return new JwtAuthResponse(token);
    }

    @Override
    public String signup(SignupRequest signupRequest) {

        logger.debug("Checking if email already exists: {}", signupRequest.getEmail());
        if(userRepository.existsByEmail(signupRequest.getEmail())) {
            logger.warn("Signup attempt with existing email: {}", signupRequest.getEmail());
            throw new EmailAlreadyExistException("A user with this email already exists " + signupRequest.getEmail());
        }

        logger.info("Creating new user with email: {}", signupRequest.getEmail());
        User user = new User();
        user.setName(signupRequest.getName());
        user.setEmail(signupRequest.getEmail());
        user.setRole("ROLE_USER"); // Normal user
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        // save the new user
        User savedUser = userRepository.save(user);
        logger.info("Successfully registered user with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        return "User registered successfully!";
    }
}
