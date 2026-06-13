# 第一阶段：构建环境 (负责编译代码)
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app
# 复制 pom.xml 和源码
COPY pom.xml .
COPY src ./src
# 执行 Maven 打包命令 (跳过测试是为了加快速度，因为CI里会单独跑测试)
RUN mvn clean package -DskipTests

# 第二阶段：运行环境 (负责运行)
FROM openjdk:17-jdk-alpine
WORKDIR /app
# 从第一阶段把打好的 jar 包复制过来
# 注意：这里的 *.jar 会自动匹配 target 目录下生成的那个文件
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]