FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/paystream-0.0.1-SNAPSHOT.jar"]