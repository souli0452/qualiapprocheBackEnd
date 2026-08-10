# Étape 1 : Build avec Maven et Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Tout le dépôt, et non les seuls modules utiles : le pom parent déclare les treize modules du
# projet, et Maven refuse de charger la construction dès qu'un répertoire déclaré manque. Ne copier
# que « common », « workflow-service » et le pom faisait donc échouer l'image sur « Child module
# lib-storage does not exist », avant même la moindre compilation.
COPY . .

# Compilation spécifique du module workflow-service
# -pl : project list (on cible workflow-service)
# -am : also make (on compile les dépendances nécessaires, dont le module common)
RUN mvn clean package -DskipTests -pl workflow-service -am

# Étape 2 : Image finale légère avec le JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# On récupère le JAR généré dans l'étape précédente
COPY --from=build /app/workflow-service/target/*.jar workflow-service.jar

EXPOSE 8092

ENTRYPOINT ["java", "-jar", "workflow-service.jar"]
