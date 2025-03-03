package com.example.kafkainit;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * ACLInitializer — инициализатор топиков и ACL (прав доступа) в Kafka.
 * Выполняется при запуске приложения перед ACLChecker.
 */
@Component
@Order(1) // Выполнится перед ACLChecker
public class ACLInitializer implements CommandLineRunner {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.ssl.truststore.location}")
    private String truststoreLocation;

    @Value("${kafka.ssl.truststore.password}")
    private String truststorePassword;

    @Value("${kafka.ssl.keystore.location}")
    private String keystoreLocation;

    @Value("${kafka.ssl.keystore.password}")
    private String keystorePassword;

    @Value("${kafka.ssl.key.password}")
    private String keyPassword;

    private final List<String> users = Arrays.asList("User:producer");

    @Override
    public void run(String... args) throws Exception {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put("security.protocol", "SASL_SSL");
        config.put("sasl.mechanism", "PLAIN");

        config.put("sasl.jaas.config",
        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"admin\" " +
        "password=\"admin-secret\";");

        config.put("ssl.truststore.location", truststoreLocation);
        config.put("ssl.truststore.password", truststorePassword);
        config.put("ssl.keystore.location", keystoreLocation);
        config.put("ssl.keystore.password", keystorePassword);
        config.put("ssl.key.password", keyPassword);

        try (AdminClient adminClient = AdminClient.create(config)) {
            createTopics(adminClient);
            createAcls(adminClient);
        }
    }

    private void createTopics(AdminClient adminClient) throws InterruptedException, ExecutionException {
        int retryCount = 0;
        final int maxRetries = 5;
        long waitTime = 3000;

        while (retryCount < maxRetries) {
            Set<String> existingTopics = adminClient.listTopics().names().get();

            List<NewTopic> topicsToCreate = new ArrayList<>();
            if (!existingTopics.contains("topic-1")) {
                topicsToCreate.add(new NewTopic("topic-1", 3, (short) 3));
            }
            if (!existingTopics.contains("topic-2")) {
                topicsToCreate.add(new NewTopic("topic-2", 3, (short) 3));
            }

            if (topicsToCreate.isEmpty()) {
                System.out.println("[INFO] Все топики уже существуют.");
                break;
            }

            try {
                adminClient.createTopics(topicsToCreate).all().get();
                System.out.println("[SUCCESS] Созданы топики: " + topicsToCreate.stream()
                        .map(NewTopic::name)
                        .collect(Collectors.joining(", ")));
                break;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                    System.out.println("[WARN] Топик уже существует. Повторяем попытку...");
                } else {
                    System.err.println("[ERROR] Ошибка при создании топиков: " + e.getMessage());
                    throw e;
                }
            }

            retryCount++;
            Thread.sleep(waitTime);
            waitTime *= 2;
        }

        if (retryCount == maxRetries) {
            System.out.println("[ERROR] Достигнуто максимальное число попыток. Некоторые топики могли не создаться.");
        }
    }

    private void createAcls(AdminClient adminClient) throws InterruptedException, ExecutionException {
        List<AclBinding> aclBindings = new ArrayList<>();

        ResourcePattern topic1 = new ResourcePattern(ResourceType.TOPIC, "topic-1", PatternType.LITERAL);
        for (String user : users) {
            aclBindings.add(new AclBinding(topic1, new AccessControlEntry(user, "*", AclOperation.CREATE, AclPermissionType.ALLOW)));
            aclBindings.add(new AclBinding(topic1, new AccessControlEntry(user, "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW)));
            aclBindings.add(new AclBinding(topic1, new AccessControlEntry(user, "*", AclOperation.READ, AclPermissionType.ALLOW)));
            aclBindings.add(new AclBinding(topic1, new AccessControlEntry(user, "*", AclOperation.WRITE, AclPermissionType.ALLOW)));
        }

        ResourcePattern topic2 = new ResourcePattern(ResourceType.TOPIC, "topic-2", PatternType.LITERAL);
        for (String user : users) {
            aclBindings.add(new AclBinding(topic2, new AccessControlEntry(user, "*", AclOperation.CREATE, AclPermissionType.ALLOW)));
            aclBindings.add(new AclBinding(topic2, new AccessControlEntry(user, "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW)));
            aclBindings.add(new AclBinding(topic2, new AccessControlEntry(user, "*", AclOperation.WRITE, AclPermissionType.ALLOW)));
        }

        for (String user : users) {
            ResourcePattern clusterResource = new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
            aclBindings.add(new AclBinding(clusterResource, new AccessControlEntry(user, "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW)));

            ResourcePattern groupResource = new ResourcePattern(ResourceType.GROUP, "group1", PatternType.LITERAL);
            aclBindings.add(new AclBinding(groupResource, new AccessControlEntry(user, "*", AclOperation.READ, AclPermissionType.ALLOW)));
            aclBindings.add(new AclBinding(groupResource, new AccessControlEntry(user, "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW)));
        }

        List<AclBinding> aclsToCreate = new ArrayList<>();
        for (AclBinding binding : aclBindings) {
            if (!aclExists(adminClient, binding)) {
                aclsToCreate.add(binding);
            }
        }

        if (!aclsToCreate.isEmpty()) {
            adminClient.createAcls(aclsToCreate).all().get();
            System.out.println("[SUCCESS] Созданы ACL:");
            for (AclBinding acl : aclsToCreate) {
                System.out.printf("[INFO] ACL: %s %s %s%n",
                        acl.entry().principal(),
                        acl.pattern().resourceType(),
                        acl.entry().operation());
            }
        } else {
            System.out.println("[INFO] Все ACL уже существуют, создание не требуется.");
        }
    }

    private boolean aclExists(AdminClient adminClient, AclBinding binding)
            throws InterruptedException, ExecutionException {
        AclBindingFilter filter = binding.toFilter();
        Collection<AclBinding> existingAcls = adminClient.describeAcls(filter).values().get();
        return !existingAcls.isEmpty();
    }
}
