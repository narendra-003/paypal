package com.paypal.user_service.service;

import com.paypal.user_service.entity.User;

import java.util.List;

public interface UserService {

    User createUser(User user);

    User getUserById(Long id);

    List<User> getAllUsers();
}
