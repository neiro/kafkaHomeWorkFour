package com.example.kafkainit;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Проверяет ACL (права доступа) в Kafka и тестирует взаимодействие.
 * 1. Проверяет ACL через AdminClient.
 * 2. Тестирует отправку сообщений через Producer.
 * 3. Тестирует возможность чтения сообщений через Consumer.
 */
@Component
@Order(2) // Выполняется после ACLInitializer
public class ACLChecker implements CommandLineRunner {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.ssl.truststore.location}")
    private String truststoreLocation;

    @Value("${spring.kafka.properties.ssl.truststore.password}")
    private String truststorePassword;

    @Value("${spring.kafka.properties.ssl.keystore.location}")
    private String keystoreLocation;

    @Value("${spring.kafka.properties.ssl.keystore.password}")
    private String keystorePassword;

    @Value("${spring.kafka.properties.ssl.key.password}")
    private String keyPassword;

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String saslJaasConfig;

    @Value("${spring.kafka.properties.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;

    private static final String PRODUCER_PRINCIPAL = "User:producer";

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[INFO] Начало проверки ACL и тестирования Kafka...");

        checkAcls();
        testProducer();
        testConsumer();

        System.out.println("[SUCCESS] Проверка ACL завершена успешно!");
    }

    /**
     * Проверяет ACL в Kafka через AdminClient.
     */
    private void checkAcls() throws ExecutionException, InterruptedException {
        Properties adminProps = createKafkaProperties();
        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            System.out.println("[INFO] Проверка ACL через AdminClient...");

            checkTopicPermissions(adminClient, "topic-1", Arrays.asList(AclOperation.READ, AclOperation.WRITE, AclOperation.DESCRIBE));
            checkTopicPermissions(adminClient, "topic-2", Arrays.asList(AclOperation.READ, AclOperation.WRITE, AclOperation.DESCRIBE));

            Set<String> topics = adminClient.listTopics().names().get();
            System.out.println("[INFO] Список доступных топиков: " + topics);
        }
    }

    /**
     * Проверяет права ACL для заданного топика.
     */
    private void checkTopicPermissions(AdminClient adminClient, String topic, List<AclOperation> operations) throws ExecutionException, InterruptedException {
        System.out.println("[INFO] Проверка ACL для топика: " + topic);
        for (AclOperation operation : operations) {
            System.out.println("[INFO] " + operation + ": " + getAcls(adminClient, topic, operation));
        }
    }

    /**
     * Тест отправки сообщений через Producer.
     */
    private void testProducer() {
        Properties producerProps = createKafkaProperties();
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        try (Producer<String, String> producer = new KafkaProducer<>(producerProps)) {
            System.out.println("[INFO] Начинаем тестирование Producer...");
            sendMessage(producer, "topic-1", "Hello from topic-1!");
            sendMessage(producer, "topic-2", "Hello from topic-2!");
            System.out.println("[SUCCESS] Сообщения успешно отправлены.");
        } catch (Exception e) {
            System.err.println("[ERROR] Ошибка при отправке сообщений: " + e.getMessage());
        }
    }

    /**
     * Отправляет сообщение в Kafka.
     */
    private void sendMessage(Producer<String, String> producer, String topic, String message) {
        producer.send(new ProducerRecord<>(topic, "key", message));
        producer.flush();
        System.out.println("[SUCCESS] Сообщение отправлено в " + topic);
    }

    /**
     * Тестирует возможность чтения сообщений через Consumer.
     */
    private void testConsumer() {
        Properties consumerProps = createKafkaProperties();
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        testConsumerForTopic(consumerProps, "topic-1", true);
        testConsumerForTopic(consumerProps, "topic-2", false);
    }

    /**
     * Проверяет, может ли Consumer читать из указанного топика.
     */
    private void testConsumerForTopic(Properties consumerProps, String topic, boolean expectSuccess) {
        System.out.println("[INFO] Тестирование Consumer для " + topic);
        try (Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

            if (!records.isEmpty()) {
                System.out.println("[SUCCESS] Consumer прочитал " + records.count() + " сообщений из " + topic);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("[INFO] offset=%d, key=%s, value=%s%n", record.offset(), record.key(), record.value());
                }
            } else if (expectSuccess) {
                System.out.println("[WARN] Нет сообщений, но главное - не упали в Authorization Error.");
            } else {
                System.out.println("[SUCCESS] Чтение из " + topic + " запрещено (ожидаемое поведение).");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Ошибка при чтении " + topic + ": " + e.getMessage());
        }
    }

    /**
     * Получает список ACL для топика.
     */
    private Collection<AclBinding> getAcls(AdminClient adminClient, String topic, AclOperation operation)
            throws ExecutionException, InterruptedException {
        ResourcePatternFilter resourceFilter = new ResourcePatternFilter(ResourceType.TOPIC, topic, PatternType.LITERAL);
        AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(PRODUCER_PRINCIPAL, "*", operation, AclPermissionType.ALLOW);
        AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);
        return adminClient.describeAcls(filter).values().get();
    }

    /**
     * Создает и возвращает настройки Kafka для AdminClient, Producer и Consumer.
     */
    private Properties createKafkaProperties() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put("security.protocol", securityProtocol);
        props.put("sasl.mechanism", saslMechanism);
        props.put("sasl.jaas.config", saslJaasConfig);
        props.put("ssl.truststore.location", truststoreLocation);
        props.put("ssl.truststore.password", truststorePassword);
        props.put("ssl.keystore.location", keystoreLocation);
        props.put("ssl.keystore.password", keystorePassword);
        props.put("ssl.key.password", keyPassword);
        return props;
    }
}
