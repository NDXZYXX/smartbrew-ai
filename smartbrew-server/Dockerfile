# ============================================================
# Smart Brew AI — Spring Boot Dockerfile (多阶段构建)
# Stage 1: Maven 构建
# Stage 2: JRE 运行
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Stage 2: Run ----
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S smartbrew && adduser -S smartbrew -G smartbrew
USER smartbrew

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
