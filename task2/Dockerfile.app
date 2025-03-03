# Этап сборки: используем образ Maven с OpenJDK 20 для компиляции приложения
FROM maven:3.9.1-eclipse-temurin-20 AS build
WORKDIR /app

# Копируем файлы конфигурации Maven (pom.xml) и исходный код
COPY pom.xml .
COPY src ./src

# Собираем Java-приложение, пропуская тесты для ускорения сборки
RUN mvn clean package -DskipTests

# Этап выполнения: используем более легковесный образ OpenJDK 21 с JRE
FROM eclipse-temurin:21
WORKDIR /app

# Устанавливаем netcat (nc) для отладки и проверки доступности сервисов
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Копируем скомпилированный JAR-файл из предыдущего этапа сборки
COPY --from=build /app/target/kafka-application.jar .

# Копируем сертификаты в контейнер
COPY certs /var/ssl/private

# Копируем вспомогательные скрипты для ожидания сертификатов и Kafka
COPY wait-for-certs.sh /app/wait-for-certs.sh
COPY wait-for-kafka.sh /app/wait-for-kafka.sh

# Копируем wrapper-скрипт, который управляет запуском приложения
COPY entrypoint_kafkainit.sh /app/entrypoint_kafkainit.sh

# Делаем скрипты исполняемыми
RUN chmod +x /app/wait-for-certs.sh /app/wait-for-kafka.sh /app/entrypoint_kafkainit.sh

# Устанавливаем точку входа на wrapper-скрипт, который подготавливает окружение перед запуском Java-приложения
ENTRYPOINT ["/app/entrypoint_kafkainit.sh"]
