package com.paypal.user_service.service;

import com.paypal.user_service.dto.JwtAuthResponse;
import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;

public interface AuthService {
    JwtAuthResponse login(LoginRequest loginRequest);
    String signup(SignupRequest signupRequest);
}
