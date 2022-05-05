FROM openjdk:17.0.2-jdk-slim
COPY ./test_image-all.jar /app/server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
