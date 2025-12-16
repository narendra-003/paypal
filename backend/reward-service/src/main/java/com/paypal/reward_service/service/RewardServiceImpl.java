package com.paypal.reward_service.service;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RewardServiceImpl implements RewardService{

    private static final Logger logger = LoggerFactory.getLogger(RewardServiceImpl.class);

    private final RewardRepository rewardRepository;

    public RewardServiceImpl(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Override
    public Reward sendReward(Reward reward) {
        logger.info("Creating reward for user: {}, amount: {}", reward.getUserId(), reward.getPoints());
        logger.debug("Reward transaction ID: {}", reward.getTransactionId());
        Reward savedReward = rewardRepository.save(reward);
        logger.info("Successfully created reward with ID: {}", savedReward.getId());
        return savedReward;
    }

    @Override
    public List<Reward> getAllRewards() {
        logger.debug("Fetching all rewards");
        List<Reward> rewards = rewardRepository.findAll();
        logger.info("Retrieved {} rewards from database", rewards.size());
        return rewards;
    }

    @Override
    public List<Reward> getRewardsByUserId(Long userId) {
        logger.debug("Fetching rewards for user: {}", userId);
        List<Reward> rewards = rewardRepository.findByUserId(userId);
        logger.info("Found {} rewards for user: {}", rewards.size(), userId);
        return rewards;
    }
}
