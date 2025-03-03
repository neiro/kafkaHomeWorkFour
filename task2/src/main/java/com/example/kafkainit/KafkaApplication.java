package com.example.kafkainit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Основной класс приложения Kafka.
 * Запускает Spring Boot приложение и инициализирует контекст.
 */
@SpringBootApplication
public class KafkaApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args Аргументы командной строки
     */
    public static void main(String[] args) {
        System.out.println("[INFO] Запуск KafkaApplication...");
        SpringApplication.run(KafkaApplication.class, args);
        System.out.println("[SUCCESS] KafkaApplication успешно запущено!");
    }
}
