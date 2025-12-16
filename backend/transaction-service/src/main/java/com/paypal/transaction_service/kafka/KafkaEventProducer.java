package com.paypal.transaction_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paypal.transaction_service.entity.Transaction;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventProducer.class);
    private static final String TOPIC = "txn-initiated";

    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public KafkaEventProducer(KafkaTemplate<String, Transaction> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendTransactionEvent(String key, Transaction transaction) {
        logger.debug("Sending to Kafka - Topic: {}, Key: {}, Transaction: {}", TOPIC, key, transaction);

        CompletableFuture<SendResult<String, Transaction>> future = kafkaTemplate.send(TOPIC, key, transaction);

        future.thenAccept(result -> {
            RecordMetadata metadata = result.getRecordMetadata();
            logger.info("Kafka message sent successfully - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    metadata.topic(), metadata.partition(), metadata.offset(), key);
        }).exceptionally(ex -> {
            logger.error("Failed to send Kafka message - Topic: {}, Key: {}, Error: {}", TOPIC, key, ex.getMessage(), ex);
            return null;
        });
    }
}
