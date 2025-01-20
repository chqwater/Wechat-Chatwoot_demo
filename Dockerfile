# 使用 OpenJDK 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 将 pom.xml 和源码复制到容器中
COPY pom.xml /app/
COPY src /app/src/

# 下载依赖并构建应用
RUN mvn clean package -DskipTests

# 暴露应用端口（Spring Boot 默认端口是 8080）
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "target/wechat-chatwoot-1.0-SNAPSHOT.jar"]
