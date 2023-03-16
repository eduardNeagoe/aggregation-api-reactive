FROM openjdk:17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} aggregation-api-reactive.jar
ENTRYPOINT ["java","-jar","/aggregation-api-reactive.jar"]
