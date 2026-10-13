# Two stages: build with the full JDK, run on the much smaller JRE image.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependencies first, so Docker can cache them and only rebuild when pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Runs the tests too (they need no database), so a failing test stops the deploy
RUN ./mvnw -B -q package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
USER app
COPY --from=build /app/target/*.jar app.jar

# Render sets PORT; application.properties reads it
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
