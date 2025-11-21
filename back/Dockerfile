# Etapa de build: usamos Maven con Java 21 para compilar el proyecto
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiamos el pom y resolvemos dependencias (esto se cachea entre builds)
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline

# Copiamos el código fuente y construimos el JAR
COPY src ./src
RUN mvn -q -e -B clean package -DskipTests

# Etapa de runtime: imagen ligera sólo con JRE 21
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos el JAR construido desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java","-jar","/app/app.jar"]
