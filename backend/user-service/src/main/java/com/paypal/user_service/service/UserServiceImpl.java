package com.paypal.user_service.service;

import com.paypal.user_service.client.WalletClient;
import com.paypal.user_service.dto.CreateWalletRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.exception.UserNotFoundException;
import com.paypal.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService{

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final WalletClient walletClient;

    public UserServiceImpl(UserRepository userRepository, WalletClient walletClient) {
        this.userRepository = userRepository;
        this.walletClient = walletClient;
    }

    @Override
    public User createUser(User user) {
        logger.info("Creating user with email: {}", user.getEmail());
        User savedUser = userRepository.save(user);
        logger.info("User saved with ID: {}", savedUser.getId());

        try {
            logger.debug("Creating wallet for user ID: {}", savedUser.getId());
            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId(savedUser.getId());
            request.setCurrency("INR");
            walletClient.createWallet(request);
            logger.info("Successfully created wallet for user ID: {}", savedUser.getId());
        } catch (Exception ex) {
            logger.error("Failed to create wallet for user ID: {}. Rolling back user creation", savedUser.getId(), ex);
            userRepository.deleteById(savedUser.getId()); // rollback
            throw new RuntimeException("Wallet creating failed, user rolled back", ex);
        }

        return savedUser;
    }

    @Override
    public User getUserById(Long id) {
        logger.debug("Fetching user with ID: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> {
            logger.warn("User not found with ID: {}", id);
            return new UserNotFoundException("User not found with id: " + id);
        });
        logger.debug("Successfully retrieved user with ID: {}", id);
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        List<User> users = userRepository.findAll();
        logger.info("Retrieved {} users from database", users.size());
        return users;
    }
}
