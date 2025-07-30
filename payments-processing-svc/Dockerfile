FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy the parent pom
COPY pom.xml .

# Copy the common pom
COPY common/pom.xml common/pom.xml

# Copy the project pom
COPY payments-processing-svc/pom.xml payments-processing-svc/pom.xml

# Filter unwanted modules and overwrite the original pom.xml
RUN sed -n '\
  1,/<modules>/p; \
  /<module>common<\/module>/p; \
  /<module>payments-processing-svc<\/module>/p; \
  /<\/modules>/,$p' pom.xml > filtered-pom.xml \
  && mv filtered-pom.xml pom.xml

# Now install the stripped-down parent POM
RUN mvn -B install:install-file \
    -Dfile=pom.xml \
    -DpomFile=pom.xml \
    -DgroupId=com.example \
    -DartifactId=csv-payments \
    -Dversion=1.0.0 \
    -Dpackaging=pom \
    -DgeneratePom=false

# Copy and install common module
COPY common/ common/
RUN mvn -B install -f common/pom.xml -DskipTests

# Copy and prepare the app module
RUN mvn -B dependency:go-offline -f payments-processing-svc/pom.xml

COPY payments-processing-svc/ payments-processing-svc/
RUN mvn -B clean package -f payments-processing-svc/pom.xml \
    -DskipTests \
    -Dquarkus.package.jar.enabled=true \
    -Dquarkus.package.jar.type=fast-jar \
    -Dquarkus.profile=prod

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/payments-processing-svc/target/quarkus-app/ .
EXPOSE 8083
CMD ["java", "-jar", "quarkus-run.jar"]
