FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml /app/

COPY src /app/src/

RUN mvn clean package -DskipTests

EXPOSE 80

CMD ["java", "-jar", "/app/target/wechat-chatwoot-1.0-SNAPSHOT.jar"]
