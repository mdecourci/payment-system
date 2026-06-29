FROM ubuntu/jdk:21-24.04_stable

WORKDIR /app

COPY target/container-1.0.0-SNAPSHOT.jar payments.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "payments.jar"]
