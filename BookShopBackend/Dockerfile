FROM maven:3.9-eclipse-temurin-25 AS compilacion
WORKDIR /proyecto
COPY . .
RUN mvn -B -pl Service -am package -DskipTests

FROM eclipse-temurin:25-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && curl -fL https://repo.maven.apache.org/maven2/org/glassfish/main/distributions/glassfish/8.0.5/glassfish-8.0.5.zip -o /tmp/glassfish.zip \
    && unzip -q /tmp/glassfish.zip -d /opt \
    && rm /tmp/glassfish.zip \
    && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/glassfish8/bin:${PATH}"
ENV AS_HOSTNAME=localhost
WORKDIR /app
COPY --from=compilacion /proyecto/Service/target/bookshop.war /app/bookshop.war
COPY despliegue/iniciar.sh /app/iniciar.sh
RUN sed -i 's/\r$//' /app/iniciar.sh && chmod +x /app/iniciar.sh \
    && sed -i 's/-Xmx512m/-Xmx256m/g' /opt/glassfish8/glassfish/domains/domain1/config/domain.xml
EXPOSE 8080
ENTRYPOINT ["/app/iniciar.sh"]
