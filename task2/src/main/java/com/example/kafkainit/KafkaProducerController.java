package com.example.kafkainit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Контроллер для отправки сообщений в Kafka.
 * Предоставляет REST API для отправки простых и сериализованных сообщений.
 */
@RestController
@RequestMapping("/api")
public class KafkaProducerController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate; // KafkaTemplate используется для отправки сообщений в Kafka

    /**
     * Отправляет простое строковое сообщение в указанный топик Kafka.
     *
     * @param topic   Название топика Kafka
     * @param key     Ключ сообщения (используется для партиционирования)
     * @param message Тело сообщения
     * @return Статус операции
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam String topic,
                              @RequestParam String key,
                              @RequestParam String message) {
        kafkaTemplate.send(topic, key, message);
        return "[SUCCESS] Message sent to " + topic;
    }

    /**
     * Отправляет сериализованное JSON-сообщение (используя объект Message) в Kafka.
     *
     * @param topic Название топика Kafka
     * @param key   Ключ сообщения
     * @param value Значение сообщения
     * @return Статус операции
     * @throws JsonProcessingException Если не удалось сериализовать объект
     */
    @PostMapping("/sendSerialized")
    public String sendSerialized(@RequestParam String topic,
                                 @RequestParam String key,
                                 @RequestParam String value) throws JsonProcessingException {
        Message msg = new Message(key, value); // Создаем объект сообщения
        String serialized = msg.serialize();   // Преобразуем объект в JSON
        kafkaTemplate.send(topic, key, serialized);
        return "[SUCCESS] Serialized message sent to " + topic;
    }
}
