package com.paypal.reward_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.reward_service.dto.TransactionDto;
import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RewardConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RewardConsumer.class);

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
                logger.debug("Reward already exists for transaction: {}", transactionDto.getId());
                return;
            }

            Reward reward = new Reward();
            reward.setUserId(transactionDto.getSenderId());
            reward.setPoints(transactionDto.getAmount() * 100);
            reward.setSentAt(LocalDateTime.now());
            reward.setTransactionId(transactionDto.getId());

            rewardRepository.save(reward);
            logger.info("Reward saved - UserId: {}, Points: {}, TransactionId: {}",
                    reward.getUserId(), reward.getPoints(), reward.getTransactionId());
        } catch (Exception ex) {
            logger.error("Failed to process transaction - TransactionId: {}, Error: {}",
                    transactionDto.getId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
