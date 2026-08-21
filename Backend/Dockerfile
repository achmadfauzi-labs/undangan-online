FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B || true
COPY src ./src
RUN ./mvnw package -Dmaven.test.skip=true -B
EXPOSE 8080
ENV STORAGE_BASE_PATH=/app/storage
ENTRYPOINT ["java","-jar","target/online-0.0.1-SNAPSHOT.jar"]
