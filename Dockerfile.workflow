FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY common ./common
COPY workflow-service ./workflow-service
COPY pom.xml ./pom.xml
RUN mvn clean package -pl workflow-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/workflow-service/target/*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
