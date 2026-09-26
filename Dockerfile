# Imagens base com versão exata (Maven, JDK, sistema): uma tag móvel como `21-jre-alpine`
# faria o mesmo build produzir imagens diferentes em dias diferentes. Atualizar é decisão
# explícita — trocar a versão aqui e rodar o passo a passo de novo.

# ---- Estágio 1: build -------------------------------------------------------------------
# O repositório Maven fica num cache do BuildKit que sobrevive entre builds: mudar uma classe
# não baixa as dependências de novo. (Uma camada com `dependency:go-offline` não bastaria: ela
# não resolve tudo o que o `package` usa, e o resto seria baixado a cada build.)
# Os testes nem compilam aqui: os de integração precisam de Docker (Testcontainers), que não
# existe dentro do build. Quem garante o código é o `mvn verify`, fora da imagem.
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -Dmaven.test.skip=true

# ---- Estágio 2: execução ----------------------------------------------------------------
# Só o JRE: sem Maven, sem código-fonte, sem compilador.
FROM eclipse-temurin:21.0.11_10-jre-alpine-3.23

# Usuário sem privilégio. Se a aplicação for comprometida, o invasor não é root no container.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app

EXPOSE 8080

# A JVM já enxerga o limite de memória do container; aqui só se define quanto dele o heap usa.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# O Actuator responde 503 quando algum indicador está DOWN (banco, disco), e o `wget` sai com
# erro em qualquer status que não seja 2xx — o código de saída já é o veredito, sem depender
# do formato do JSON. O `start-period` cobre a subida do Spring e as migrations do Flyway.
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
