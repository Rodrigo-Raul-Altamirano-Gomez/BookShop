# Backend BookShop: WAR Jakarta EE 11 (JAX-RS) sobre GlassFish 8
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# El runtime es Java 21; se compila para esa versión
RUN mvn -B -q -DskipTests -Dmaven.compiler.release=21 package

FROM eclipse-temurin:21-jre
# Hora de Perú: las fechas de los pedidos y del seguimiento las genera Java (LocalDateTime.now())
ENV TZ=America/Lima
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL -o /tmp/gf.zip https://download.eclipse.org/ee4j/glassfish/glassfish-8.0.0.zip \
 && unzip -q /tmp/gf.zip -d /opt && rm /tmp/gf.zip
ENV PATH="/opt/glassfish8/bin:${PATH}"
COPY --from=build /src/bookshop-rs/target/bookshop.war /tmp/bookshop.war
# Se configura y se despliega el WAR al construir la imagen (si falla, falla el build).
# Se quitan los listeners IIOP (no se usan y Render los sondea generando errores) y se acota la memoria.
RUN asadmin start-domain \
 && asadmin delete-jvm-options '-Xmx512m' \
 && asadmin create-jvm-options '-Xmx320m' \
 && (asadmin delete-iiop-listener orb-listener-1 || true) \
 && (asadmin delete-iiop-listener SSL || true) \
 && (asadmin delete-iiop-listener SSL_MUTUALAUTH || true) \
 && asadmin deploy --name bookshop --contextroot bookshop /tmp/bookshop.war \
 && asadmin list-applications \
 && asadmin stop-domain
EXPOSE 8080
# Variables: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
CMD ["asadmin", "start-domain", "--verbose"]
