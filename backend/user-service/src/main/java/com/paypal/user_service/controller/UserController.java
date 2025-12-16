package com.paypal.user_service.controller;

import com.paypal.user_service.entity.User;
import com.paypal.user_service.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        logger.info("Received request to create user with email: {}", user.getEmail());
        User savedUser = userService.createUser(user);
        logger.info("Successfully created user with ID: {}", savedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        logger.info("Received request to fetch user with ID: {}", id);
        User dbUser = userService.getUserById(id);
        logger.debug("Retrieved user: {}", dbUser.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(dbUser);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("Received request to fetch all users");
        List<User> userList = userService.getAllUsers();
        logger.info("Retrieved {} users", userList.size());
        return ResponseEntity.status(HttpStatus.OK).body(userList);
    }

}
