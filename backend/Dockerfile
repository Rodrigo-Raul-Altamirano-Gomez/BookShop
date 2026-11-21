# Backend BookShop: WAR Jakarta EE 11 (JAX-RS) sobre GlassFish 8
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# El runtime es Java 21; se compila para esa versión
RUN mvn -B -q -DskipTests -Dmaven.compiler.release=21 package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL -o /tmp/gf.zip https://download.eclipse.org/ee4j/glassfish/glassfish-8.0.0.zip \
 && unzip -q /tmp/gf.zip -d /opt && rm /tmp/gf.zip
ENV PATH="/opt/glassfish8/bin:${PATH}"
# Memoria acotada para planes pequeños
RUN asadmin start-domain \
 && asadmin delete-jvm-options '-Xmx512m' \
 && asadmin create-jvm-options '-Xmx320m' \
 && asadmin set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080 \
 && asadmin stop-domain
COPY --from=build /src/bookshop-rs/target/bookshop.war /opt/glassfish8/glassfish/domains/domain1/autodeploy/bookshop.war
EXPOSE 8080
# Variables: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
CMD ["asadmin", "start-domain", "--verbose"]
