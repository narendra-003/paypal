package com.paypal.user_service.controller;

import com.paypal.user_service.dto.JwtAuthResponse;
import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.payload.ApiResponse;
import com.paypal.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());
        JwtAuthResponse jwtAuthResponse = authService.login(loginRequest);
        logger.info("Successful login for email: {}", loginRequest.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(jwtAuthResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        logger.info("Signup attempt for email: {}", signupRequest.getEmail());
        String response = authService.signup(signupRequest);
        logger.info("Successfully registered user with email: {}", signupRequest.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(response));
    }
}
