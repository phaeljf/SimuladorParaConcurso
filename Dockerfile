FROM eclipse-temurin:21-jdk

WORKDIR /app

# copia o JAR gerado localmente
COPY target/simulador-paraconcurso-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
