import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

/**
 * Управляет запуском Kafka Consumer после загрузки приложения.
 * Ждет завершения ACLInitializer перед включением слушателей Kafka.
 * НАДО бы прикрутить окончательно, чтобы при старте пока не отраьотало ACLInitializer консьюмеры не ругались из прав доступа
 */
@Component
public class KafkaConsumerManager implements ApplicationListener<ContextRefreshedEvent> {

    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    /**
     * Внедрение реестра Kafka Listener-ов.
     *
     * @param kafkaListenerEndpointRegistry Реестр всех Kafka Listener-ов
     */
    public KafkaConsumerManager(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    }

    /**
     * Вызывается после полной инициализации контекста Spring.
     * Ожидает завершения ACLInitializer перед запуском Kafka Listener-ов.
     *
     * @param event Событие завершения загрузки контекста Spring
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("[INFO] Ожидание завершения ACLInitializer перед включением Kafka Listener...");

        try {
            Thread.sleep(5000); // Ожидание 5 секунд перед запуском (можно заменить на более надежный механизм)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ERROR] Ошибка во время ожидания запуска Kafka Listener: " + e.getMessage());
        }

        System.out.println("[SUCCESS] Включаем Kafka Consumer!");
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(container -> container.start());
    }
}
