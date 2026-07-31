# Build stage: compila com Maven + JDK completo, mas esse layer nao vai pra imagem final.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

# Copia so o pom primeiro pra cachear as dependencias numa layer separada -
# so re-baixa tudo se o pom mudar, nao a cada alteracao de codigo.
# lombok.config precisa vir junto com o pom: sem ele, o Lombok nao copia
# @Value pros construtores gerados (ver decisoes-tecnicas do projeto) e o
# Spring falha ao subir com "No qualifying bean of type 'java.lang.String'"
# em qualquer classe que injete uma property via campo final.
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./
RUN ./mvnw -q dependency:go-offline

COPY src/ src/
# Sem testes aqui de proposito: os testes de integracao precisam de Docker
# (Testcontainers) pra subir Postgres, o que nao esta disponivel dentro do
# proprio build da imagem. Teste roda no CI (ver .github/workflows/ci.yml),
# antes da imagem ser construida.
RUN ./mvnw -q -DskipTests package

# Runtime stage: so o JRE, sem Maven/JDK completo - imagem final bem menor.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --uid 1000 appuser
USER appuser

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
