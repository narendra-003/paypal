package com.paypal.reward_service.service;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RewardServiceImpl implements RewardService{
    private final RewardRepository rewardRepository;

    public RewardServiceImpl(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Override
    public Reward sendReward(Reward reward) {
        Reward savedReward = rewardRepository.save(reward);
        return savedReward;
    }

    @Override
    public List<Reward> getAllRewards() {
        List<Reward> rewards = rewardRepository.findAll();
        return rewards;
    }

    @Override
    public List<Reward> getRewardsByUserId(Long userId) {
        List<Reward> rewards = rewardRepository.findByUserId(userId);
        return rewards;
    }
}
