FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copia todo el proyecto (incluyendo el pom.xml y la carpeta ci/settings.xml)
COPY . .

# --- INICIO DE LA CORRECCIÓN ---
#
# Usamos el flag '-s ci/settings.xml' para forzar a Maven
# a usar los mirrors HTTP definidos en ese archivo.
#
RUN mvn -s ci/settings.xml clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8
#
# --- FIN DE LA CORRECCIÓN ---


FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]