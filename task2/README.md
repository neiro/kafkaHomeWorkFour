# Настройка защищённого соединения и управление доступом

## 1. Обзор

В данном проекте демонстрируется:

- Настройка кластера Kafka из трёх брокеров (`kafka1`, `kafka2`, `kafka3`) и Zookeeper с использованием Docker Compose.
- Генерация корневого сертификата и сертификатов для каждого брокера (Keystore/Truststore).
- Настройка SSL (SASL_SSL) для безопасного соединения с брокерами.
- Создание топиков `topic-1` и `topic-2`.
- Управление правами доступа (ACL), при котором:
  - `topic-1`: доступен для продюсеров и консьюмеров.
  - `topic-2`: доступен только для продюсеров (чтение/доступ консьюмерам запрещён).
- Проверка отправки и чтения (или отсутствия чтения) сообщений через встроенные продюсер и консьюмер (Spring Boot-приложение).

## 2. Содержимое репозитория

- **docker-compose.yml**  
  Главный файл конфигурации контейнеров (Zookeeper, три брокера Kafka, утилита для генерации сертификатов `cert-generator` и Java-приложение `kafka-acl-init`).

- **certs/**  
  Папка для хранения сгенерированных сертификатов (Truststore и Keystore для каждого брокера, а также для продюсера).

- **entrypoint_kafka.sh**, **wait-for-certs.sh**, **wait-for-kafka.sh**  
  Скрипты, обеспечивающие:
  - ожидание готовности сертификатов;
  - ожидание корректного запуска брокеров Kafka;
  - запуск Kafka с учётом SSL и SASL настроек.

- **pom.xml** и **java-исходники** (пакеты `com.example.kafkainit`)  
  Spring Boot-приложение (модуль `kafka-acl-init`), которое при запуске:
  - Создаёт требуемые топики (`topic-1`, `topic-2`).
  - Настраивает ACL (права доступа).
  - Тестирует отправку/чтение сообщений через продюсер/консьюмер.

- **application.yaml**  
  Конфигурация Spring Boot (подключение к Kafka, SSL-свойства, механизмы SASL и т.д.).

- **Файлы конфигурации JAAS**  
  Подключаются брокерами для аутентификации (SASL PLAIN) и авторизации (ACL).

## 3. Основные шаги настройки

### Шаг 1. Генерация сертификатов

В директории `certs/` запускается контейнер `cert-generator` из `docker-compose.yml`, который:

- Создаёт корневой сертификат (CA).
- Генерирует Keystore/Truststore для каждого брокера (`kafka1`, `kafka2`, `kafka3`) и клиента (`producer`).
- Выкладывает готовые `.jks`-файлы и метку `done.txt` для сигнала о завершении.
```log
2025-03-03 23:47:51 Generating a RSA private key
2025-03-03 23:47:51 ......................+++++
2025-03-03 23:47:51 ................................................+++++
2025-03-03 23:47:51 writing new private key to 'ca-key'
2025-03-03 23:47:51 -----
2025-03-03 23:47:52 Signature ok
2025-03-03 23:47:52 subject=CN = kafka1
2025-03-03 23:47:52 Getting CA Private Key
2025-03-03 23:47:51 === Создание корневого сертификата (CA) ===
2025-03-03 23:47:51 === Генерация сертификатов для kafka1 ===
2025-03-03 23:47:53 Certificate was added to keystore
2025-03-03 23:47:54 Certificate reply was installed in keystore
2025-03-03 23:47:54 Certificate was added to keystore
2025-03-03 23:47:54 Добавлен kafka_server_jaas.conf в kafka1
2025-03-03 23:47:54 === Генерация сертификатов для kafka2 ===
2025-03-03 23:47:55 Signature ok
2025-03-03 23:47:55 subject=CN = kafka2
2025-03-03 23:47:55 Getting CA Private Key
2025-03-03 23:47:56 Certificate was added to keystore
2025-03-03 23:47:57 Certificate reply was installed in keystore
2025-03-03 23:47:57 Certificate was added to keystore
2025-03-03 23:47:57 Добавлен kafka_server_jaas.conf в kafka2
2025-03-03 23:47:57 === Генерация сертификатов для kafka3 ===
2025-03-03 23:47:58 Signature ok
2025-03-03 23:47:58 subject=CN = kafka3
2025-03-03 23:47:58 Getting CA Private Key
2025-03-03 23:47:59 Certificate was added to keystore
2025-03-03 23:48:00 Certificate reply was installed in keystore
2025-03-03 23:48:00 Certificate was added to keystore
2025-03-03 23:48:00 Добавлен kafka_server_jaas.conf в kafka3
2025-03-03 23:48:00 === Генерация сертификатов для клиента (producer) ===
2025-03-03 23:48:02 Signature ok
2025-03-03 23:48:02 subject=CN = producer
2025-03-03 23:48:02 Getting CA Private Key
2025-03-03 23:48:02 Certificate was added to keystore
2025-03-03 23:48:03 Certificate reply was installed in keystore
2025-03-03 23:48:03 Certificate was added to keystore
2025-03-03 23:48:04 Сертификаты успешно сгенерированы.
```

### Шаг 2. Запуск кластера Kafka и Zookeeper с SSL

- Zookeeper запускается с параметрами SASL и принудительной аутентификацией.
- Kafka-брокеры имеют:
  - SSL-конфигурацию (Keystore и Truststore).
  - `KAFKA_OPTS` указывают на конфигурационные файлы JAAS, чтобы активировать механизм SASL/PLAIN.
  - Параметры `KAFKA_SASL_ENABLED_MECHANISMS` и `KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL` определяют, как брокеры общаются друг с другом по SSL + SASL.

### Шаг 3. ACL и топики

Приложение `kafka-acl-init` (Java/Spring Boot) при старте выполняет:

- Создание топиков `topic-1` и `topic-2` (при необходимости, с несколькими репликами/партициями).
- Назначение прав доступа:
  - `topic-1`: ALLOW для READ/WRITE и DESCRIBE.
  - `topic-2`: ALLOW для WRITE, но без READ (консьюмеры не могут читать).
- Тестирование: отправка сообщений в оба топика, чтение из `topic-1` (разрешено), попытка чтения из `topic-2` (должна блокироваться ACL).

## 4. Как запустить

1. Клонируйте репозиторий:

```bash
    git clone https://github.com/neiro/kafkaHomeWorkFour
    cd kafkaHomeWorkFour
```
2. Запустите Docker Compose:

```bash
    docker-compose up --build
```

Контейнер cert-generator сгенерирует сертификаты.

Zookeeper и три брокера Kafka поднимутся с SSL/SASL-настройками.

Java-приложение kafka-acl-init автоматически создаст топики и ACL, затем протестирует продюсер/консьюмер.

3. Убедитесь, что всё поднялось:

В логах должно отобразиться успешное создание топиков topic-1 и topic-2.

ACL проставлены, и сообщение об успехе.

Producer отправляет сообщения в оба топика, Consumer читает из topic-1 и НЕ может читать topic-2.
```log
2025-03-03 23:42:39 [SUCCESS] Созданы топики: topic-1, topic-2
2025-03-03 23:42:40 [SUCCESS] Созданы ACL:
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC CREATE
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC DESCRIBE
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC READ
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC WRITE
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC CREATE
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC DESCRIBE
2025-03-03 23:42:40 [INFO] ACL: User:producer TOPIC WRITE
2025-03-03 23:42:40 [INFO] ACL: User:producer CLUSTER DESCRIBE
2025-03-03 23:42:40 [INFO] ACL: User:producer GROUP READ
2025-03-03 23:42:40 [INFO] ACL: User:producer GROUP DESCRIBE
2025-03-03 23:42:40 [INFO] Начало проверки ACL и тестирования Kafka...
2025-03-03 23:42:40 [INFO] Проверка ACL через AdminClient...
2025-03-03 23:42:40 [INFO] Проверка ACL для топика: topic-1
2025-03-03 23:42:40 [INFO] READ: [(pattern=ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL), entry=(principal=User:producer, host=*, operation=READ, permissionType=ALLOW))]
2025-03-03 23:42:40 [INFO] WRITE: [(pattern=ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL), entry=(principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW))]
2025-03-03 23:42:40 [INFO] DESCRIBE: [(pattern=ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL), entry=(principal=User:producer, host=*, operation=DESCRIBE, permissionType=ALLOW))]
2025-03-03 23:42:40 [INFO] Проверка ACL для топика: topic-2
2025-03-03 23:42:40 [INFO] READ: []
2025-03-03 23:42:40 [INFO] WRITE: [(pattern=ResourcePattern(resourceType=TOPIC, name=topic-2, patternType=LITERAL), entry=(principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW))]
2025-03-03 23:42:40 [INFO] DESCRIBE: [(pattern=ResourcePattern(resourceType=TOPIC, name=topic-2, patternType=LITERAL), entry=(principal=User:producer, host=*, operation=DESCRIBE, permissionType=ALLOW))]
2025-03-03 23:42:40 [INFO] Список доступных топиков: [topic-1, topic-2]
2025-03-03 23:42:40 [INFO] Начинаем тестирование Producer...
2025-03-03 23:42:40 [SUCCESS] Сообщение отправлено в topic-1
2025-03-03 23:42:40 [SUCCESS] Сообщение отправлено в topic-2
2025-03-03 23:42:40 [SUCCESS] Сообщения успешно отправлены.
2025-03-03 23:42:40 [INFO] Тестирование Consumer для topic-1
2025-03-03 23:42:45 [SUCCESS] Consumer прочитал 1 сообщений из topic-1
2025-03-03 23:42:45 [INFO] offset=0, key=key, value=Hello from topic-1!
2025-03-03 23:42:45 [INFO] Тестирование Consumer для topic-2
2025-03-03 23:42:48 2025-03-03 18:42:48 WARN  o.a.k.c.consumer.internals.Fetcher - [Consumer clientId=consumer-group1-4, groupId=group1] Not authorized to read from partition topic-2-0.
2025-03-03 23:42:48 2025-03-03 18:42:48 ERROR o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-group1-4, groupId=group1] Offset commit failed on partition topic-2-0 at offset 0: Topic authorization failed.
2025-03-03 23:42:48 2025-03-03 18:42:48 ERROR o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-group1-4, groupId=group1] Not authorized to commit to topics [topic-2]
2025-03-03 23:42:48 2025-03-03 18:42:48 WARN  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-group1-4, groupId=group1] Synchronous auto-commit of offsets {topic-2-0=OffsetAndMetadata{offset=0, leaderEpoch=null, metadata=''}} failed: Not authorized to access topics: [topic-2]
2025-03-03 23:42:48 [SUCCESS] Проверка ACL завершена успешно!
2025-03-03 23:42:48 [ERROR] Ошибка при чтении topic-2: Not authorized to access topics: [topic-2]
```

   ## 5. Как проверить вручную

После успешного старта вы можете проверить работоспособность топиков и ACL любым удобным клиентом Kafka (например, консольными утилитами `kafka-console-producer`/`kafka-console-consumer`), указав SSL-свойства. Или воспользоваться REST-эндпоинтами из Spring Boot-приложения (например, `POST /api/send?topic=topic-1&key=foo&message=bar`).

## 6. Структура сертификатов

В папке `certs/` создаются следующие файлы:

- `ca-cert`, `ca-key` – корневой сертификат и ключ CA.
- `kafka1/`, `kafka2/`, `kafka3/`, `producer/` – индивидуальные keystore/truststore (`.keystore.jks`, `.truststore.jks`), а также служебные `done.txt`, указывающие, что сертификаты готовы.

## 7. Итоги

- Настроен трёхброкерный кластер Kafka с SSL (SASL_SSL).
- Созданы топики `topic-1` и `topic-2`, проверен доступ:
  - `topic-1`: доступен для чтения и записи.
  - `topic-2`: доступен только для записи.
- Готовые сертификаты можно перенести на другой компьютер для запуска аналогичного окружения.
