FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy the parent pom
COPY pom.xml .

# Copy the common pom
COPY common/pom.xml common/pom.xml

# Copy the project pom
COPY orchestrator-svc/pom.xml orchestrator-svc/pom.xml

# Filter unwanted modules and overwrite the original pom.xml
RUN sed -n '\
  1,/<modules>/p; \
  /<module>common<\/module>/p; \
  /<module>orchestrator-svc<\/module>/p; \
  /<\/modules>/,$p' pom.xml > filtered-pom.xml \
  && mv filtered-pom.xml pom.xml

# Now install the stripped-down parent POM
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B install:install-file \
    -Dfile=pom.xml \
    -DpomFile=pom.xml \
    -DgroupId=io.github.mbarcia \
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
    mvn -B dependency:go-offline -f orchestrator-svc/pom.xml

COPY orchestrator-svc/ orchestrator-svc/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -f orchestrator-svc/pom.xml \
    -DskipTests \
    -Dquarkus.package.jar.enabled=true \
    -Dquarkus.package.jar.type=fast-jar \
    -Dquarkus.profile=prod

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/orchestrator-svc/target/quarkus-app/ .

# Add JVM tuning options for Docker environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

CMD ["sh", "-c", "java $JAVA_OPTS --enable-preview -jar quarkus-run.jar"]
