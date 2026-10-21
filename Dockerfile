# Этап 1: Сборка
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Собираем проект
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный JAR (shade-plugin переименовывает его в mysite-1.0.jar)
COPY --from=build /app/target/mysite-1.0.jar app.jar

# Копируем фронтенд и данные
COPY frontend ./frontend
COPY data ./data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]