FROM maven:3.9-eclipse-temurin-25 AS compilacion
WORKDIR /proyecto
COPY . .
RUN mvn -B -pl Service -am package -DskipTests

FROM eclipse-temurin:25-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && curl -fL https://repo.maven.apache.org/maven2/org/glassfish/main/distributions/web/8.0.5/web-8.0.5.zip -o /tmp/glassfish.zip \
    && unzip -q /tmp/glassfish.zip -d /opt \
    && rm /tmp/glassfish.zip \
    && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/glassfish8/bin:${PATH}"
ENV AS_HOSTNAME=localhost
# Las fechas del negocio (LocalDateTime.now) usan la hora de Lima.
ENV TZ=America/Lima
WORKDIR /app
COPY --from=compilacion /proyecto/Service/target/bookshop.war /app/bookshop.war
RUN DOMINIO=/opt/glassfish8/glassfish/domains/domain1; \
    sed -i 's/-Xmx512m/-Xmx256m/g' "$DOMINIO/config/domain.xml" && \
    sed -i '/<network-listener .*protocol="admin-listener"/s/ address="[^"]*"//g' "$DOMINIO/config/domain.xml" && \
    sed -i '/<network-listener .*protocol="http-listener-2"/s/ address="[^"]*"//g' "$DOMINIO/config/domain.xml" && \
    sed -i '/<network-listener /s/protocol="admin-listener"/address="127.0.0.1" protocol="admin-listener"/' "$DOMINIO/config/domain.xml" && \
    sed -i '/<network-listener /s/protocol="http-listener-2"/address="127.0.0.1" protocol="http-listener-2"/' "$DOMINIO/config/domain.xml" && \
    sed -i '/<iiop-listener /s/ enabled="[^"]*"//g; /<iiop-listener /s/<iiop-listener /<iiop-listener enabled="false" /; /<iiop-listener /s/lazy-init="true"/lazy-init="false"/g' "$DOMINIO/config/domain.xml" && \
    sed -i '/<jmx-connector /s/address="[^"]*"/address="127.0.0.1"/g' "$DOMINIO/config/domain.xml"
EXPOSE 8080

# Render proporciona la configuración de MySQL y el puerto al arrancar.
CMD set -eu; \
    umask 077; \
    mkdir -p /run/bookshop/WEB-INF/classes; \
    printf '%s' "$BOOKSHOP_DB_PROPERTIES_B64" | base64 -d > /run/bookshop/WEB-INF/classes/db.properties; \
    jar --update --file /app/bookshop.war -C /run/bookshop WEB-INF/classes/db.properties; \
    DOMINIO=/opt/glassfish8/glassfish/domains/domain1; \
    sed -i "s/port=\"8080\"/port=\"${PORT:-8080}\"/g" "$DOMINIO/config/domain.xml"; \
    cp /app/bookshop.war "$DOMINIO/autodeploy/bookshop.war"; \
    exec asadmin start-domain --verbose domain1
