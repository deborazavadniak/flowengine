# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — BUILD
# Compila o projeto e roda os testes dentro do container.
# Esta imagem é descartada no final — não vai para produção.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copia SOMENTE o pom.xml primeiro.
# O Docker cacheia esta camada separadamente.
# Se o pom.xml não mudar, o Maven não baixa as dependências de novo.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Agora copia o código-fonte e compila
COPY src ./src
RUN mvn clean package -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — RUNTIME
# Imagem final enxuta com apenas o JRE (sem Maven, sem código-fonte).
# eclipse-temurin:21-jre-alpine = ~190MB vs ~600MB do JDK completo.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Cria um usuário não-root por segurança.
# Rodar como root dentro do container é má prática.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copia APENAS o JAR gerado no stage de build.
# Tudo o mais (Maven, JDK, código-fonte) fica para trás.
COPY --from=build /app/target/flowengine-*.jar app.jar

# Porta que a aplicação vai escutar (deve bater com server.port no application.yml)
EXPOSE 8080

# Comando de inicialização com flags otimizadas para containers:
# -XX:+UseContainerSupport   → JVM respeita os limites de CPU/memória do container
# -XX:MaxRAMPercentage=75.0  → usa no máximo 75% da RAM disponível para o heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]