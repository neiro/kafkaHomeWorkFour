#!/bin/bash
set -e  # Прекращает выполнение скрипта при ошибке любой команды

# Ожидание сертификатов перед запуском приложения
/app/wait-for-certs.sh

# Ожидание доступности Kafka перед запуском приложения
/app/wait-for-kafka.sh

echo "Ожидание наличия сертификатов"
# Цикл ожидания, пока не появится файл-индикатор завершения подготовки сертификатов
while [ ! -f /var/ssl/private/done.txt ]; do
  sleep 2  # Проверяем наличие файла каждые 2 секунды
done
echo "Файл найден, запуск Java-приложения..."

# Ждем дополнительное время, чтобы кластер Kafka стабилизировался перед запуском приложения
echo "Ожидание 15 секунд для стабилизации кластера Kafka перед запуском Java-приложения..."
sleep 15

# Запуск Java-приложения
exec java -jar kafka-application.jar
