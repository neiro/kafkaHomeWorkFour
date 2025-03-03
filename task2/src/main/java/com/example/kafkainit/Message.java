package com.example.kafkainit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Класс Message представляет сообщение в Kafka.
 * Используется для сериализации и десериализации сообщений в JSON.
 */
public class Message {

    private String key;
    private String value;

    /**
     * Конструктор без параметров (нужен для десериализации Jackson).
     */
    public Message() {}

    /**
     * Конструктор для создания сообщения с ключом и значением.
     *
     * @param key   Ключ сообщения
     * @param value Значение сообщения
     */
    public Message(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    /**
     * Преобразует объект Message в JSON-строку.
     *
     * @return JSON-представление объекта Message
     * @throws JsonProcessingException Если не удалось сериализовать объект
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    /**
     * Создает объект Message из JSON-строки.
     *
     * @param json JSON-строка, содержащая данные Message
     * @return Объект Message
     * @throws JsonProcessingException Если не удалось десериализовать JSON
     */
    public static Message deserialize(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Message.class);
    }

    /**
     * Возвращает строковое представление объекта Message.
     *
     * @return Строка в формате "Message {key='...', value='...'}"
     */
    @Override
    public String toString() {
        return "Message {key='" + key + "', value='" + value + "'}";
    }
}
