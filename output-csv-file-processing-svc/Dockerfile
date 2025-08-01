FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy the parent pom
COPY pom.xml .

# Copy the common pom
COPY common/pom.xml common/pom.xml

# Copy the project pom
COPY output-csv-file-processing-svc/pom.xml output-csv-file-processing-svc/pom.xml

# Filter unwanted modules and overwrite the original pom.xml
RUN sed -n '\
  1,/<modules>/p; \
  /<module>common<\/module>/p; \
  /<module>output-csv-file-processing-svc<\/module>/p; \
  /<\/modules>/,$p' pom.xml > filtered-pom.xml \
  && mv filtered-pom.xml pom.xml

# Now install the stripped-down parent POM
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B install:install-file \
    -Dfile=pom.xml \
    -DpomFile=pom.xml \
    -DgroupId=com.example \
    -DartifactId=csv-payments \
    -Dversion=1.0.0 \
    -Dpackaging=pom \
    -DgeneratePom=false

# Copy and install common module
COPY common/ common/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B install -f common/pom.xml -DskipTests

# Copy and prepare the app module
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline -f output-csv-file-processing-svc/pom.xml

COPY output-csv-file-processing-svc/ output-csv-file-processing-svc/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -f output-csv-file-processing-svc/pom.xml \
    -DskipTests \
    -Dquarkus.package.jar.enabled=true \
    -Dquarkus.package.jar.type=fast-jar \
    -Dquarkus.profile=prod

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/output-csv-file-processing-svc/target/quarkus-app/ .
CMD ["java", "--enable-preview", "-jar", "quarkus-run.jar"]
