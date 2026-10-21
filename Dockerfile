# Этап 1: Сборка приложения с помощью Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости (для кеширования)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код и собираем приложение
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный JAR-файл из первого этапа
COPY --from=build /app/target/*.jar app.jar

# Копируем папки с фронтендом и данными
COPY frontend ./frontend
COPY data ./data

# Открываем порт (Render сам подставит нужный)
EXPOSE 8080

# Команда для запуска
ENTRYPOINT ["java", "-jar", "app.jar"]