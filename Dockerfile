# 使用 openjdk 17 镜像作为基础镜像
FROM openjdk:17-jdk-slim

# 安装 Maven
RUN apt-get update && apt-get install -y maven

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 文件
COPY pom.xml /app/

# 复制 src 目录
COPY src /app/src/

# 使用 Maven 构建项目（跳过测试）
RUN mvn clean package -DskipTests

# 暴露端口（Spring Boot 默认端口是 8080）
EXPOSE 80

# 启动应用
CMD ["java", "-jar", "/app/target/wechat-chatwoot-1.0-SNAPSHOT.jar"]
