package com.paypal.reward_service.controller;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.service.RewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rewards/")
public class RewardController {

    private static final Logger logger = LoggerFactory.getLogger(RewardController.class);

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public ResponseEntity<List<Reward>> getAllRewards() {
        logger.info("Received request to fetch all rewards");
        List<Reward> rewards = rewardService.getAllRewards();
        logger.info("Retrieved {} rewards", rewards.size());
        return ResponseEntity.status(HttpStatus.OK).body(rewards);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reward>> getRewardByUserId(@PathVariable Long userId) {
        logger.info("Received request to fetch rewards for user: {}", userId);
        List<Reward> rewards = rewardService.getRewardsByUserId(userId);
        logger.info("Retrieved {} rewards for user: {}", rewards.size(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(rewards);
    }
}
