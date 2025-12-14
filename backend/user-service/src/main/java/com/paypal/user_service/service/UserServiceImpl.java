package com.paypal.user_service.service;

import com.paypal.user_service.client.WalletClient;
import com.paypal.user_service.dto.CreateWalletRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.exception.UserNotFoundException;
import com.paypal.user_service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final WalletClient walletClient;

    public UserServiceImpl(UserRepository userRepository, WalletClient walletClient) {
        this.userRepository = userRepository;
        this.walletClient = walletClient;
    }

    @Override
    public User createUser(User user) {
        User savedUser = userRepository.save(user);

        try {
            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId(savedUser.getId());
            request.setCurrency("INR");
            walletClient.createWallet(request);
        } catch (Exception ex) {
            userRepository.deleteById(savedUser.getId()); // rollback
            throw new RuntimeException("Wallet creating failed, user rolled back", ex);
        }

        return savedUser;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
