# ==========================================
# Etapa 1: Build da aplicação com Maven e Java 21
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache de dependências Maven
COPY pom.xml .
RUN mvn dependency:resolve -B

# Compilação e empacotamento da aplicação
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Etapa 2: Imagem leve de execução (JRE 21 Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário de sistema sem privilégios de root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Criação do diretório para fotos persistentes com permissões adequadas
RUN mkdir -p /app/catalogo-fotos && chown -R appuser:appgroup /app

# Cópia do executável gerado na etapa anterior
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
