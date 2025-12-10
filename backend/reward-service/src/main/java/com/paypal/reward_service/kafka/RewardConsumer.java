package com.paypal.reward_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.reward_service.dto.TransactionDto;
import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RewardConsumer {
    private final RewardRepository rewardRepository;
    private final ObjectMapper objectMapper;


    public RewardConsumer(RewardRepository rewardRepository, ObjectMapper objectMapper) {
        this.rewardRepository = rewardRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "txn-initiated", groupId = "reward-group")
    public void consumeTransaction(TransactionDto transactionDto) {

        try {
            if(rewardRepository.existsByTransactionId(transactionDto.getId())) {
                System.out.println("Reward already exists for transaction: " + transactionDto.getId());
                return;
            }

            Reward reward = new Reward();
            reward.setUserId(transactionDto.getSenderId());
            reward.setPoints(transactionDto.getAmount() * 100);
            reward.setSentAt(LocalDateTime.now());
            reward.setTransactionId(transactionDto.getId());

            rewardRepository.save(reward);
            System.out.println("Reward saved: " + reward);
        } catch (Exception ex) {
            System.err.println("Failed to process transaction " + transactionDto.getId() + ": " + ex.getMessage());
            throw ex;
        }
    }
}
