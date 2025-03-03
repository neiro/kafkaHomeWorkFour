package com.example.kafkainit;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * KafkaConsumerListener - слушатель сообщений Kafka.
 * Подписывается на топики и обрабатывает входящие сообщения.
 */
@Component
public class KafkaConsumerListener {

    /**
     * Обрабатывает сообщения из "topic-1".
     *
     * @param message Полученное сообщение
     */
    @KafkaListener(topics = "topic-1", groupId = "group1")
    public void listenTopic1(String message) {
        System.out.println("[INFO] Получено сообщение из topic-1: " + message);
    }

    /**
     * Обрабатывает сообщения из "topic-2".
     *
     * @param message Полученное сообщение
     */
    @KafkaListener(topics = "topic-2", groupId = "group1")
    public void listenTopic2(String message) {
        System.out.println("[INFO] Получено сообщение из topic-2: " + message);
    }
}
