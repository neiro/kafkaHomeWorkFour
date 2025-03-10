#!/bin/bash
set -e  # Прекращает выполнение скрипта при ошибке любой команды

echo "Ожидание доступности брокеров Kafka..."

# Проверяем доступность каждого брокера на порту 9093.
# Если хотя бы один брокер недоступен, повторяем проверку каждые 5 секунд.
while ! nc -z kafka1 9093 || ! nc -z kafka2 9093 || ! nc -z kafka3 9093; do
    echo "Брокеры еще не доступны, ждем 5 секунд..."
    sleep 5
done

echo "Все брокеры доступны!"
# Запускаем переданный процесс
exec "$@"
