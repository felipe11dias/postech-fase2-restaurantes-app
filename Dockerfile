# ---- Estágio 1: build -------------------------------------------------------------------
# As dependências são baixadas numa camada própria, antes do código: mudar uma classe não
# invalida o cache delas, e o rebuild leva segundos em vez de minutos.
# Os testes não rodam aqui: os de integração precisam de Docker (Testcontainers), que não
# existe dentro do build. Quem garante o código é o `mvn verify`, fora da imagem.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Estágio 2: execução ----------------------------------------------------------------
# Só o JRE: sem Maven, sem código-fonte, sem compilador.
FROM eclipse-temurin:21-jre-alpine

# Usuário sem privilégio. Se a aplicação for comprometida, o invasor não é root no container.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app

EXPOSE 8080

# A JVM já enxerga o limite de memória do container; aqui só se define quanto dele o heap usa.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Saudável = a aplicação responde e alcança o banco. O `start-period` cobre a subida do Spring
# e as migrations do Flyway, para o container não ser dado como doente enquanto inicia.
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
