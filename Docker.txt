# Java 17 (Spring Boot compatible)
FROM eclipse-temurin:17-jdk

# App ka working directory
WORKDIR /app

# Saara project copy karo
COPY . .

# Maven wrapper ko executable banao
RUN chmod +x mvnw

# Build Spring Boot app
RUN ./mvnw clean package -DskipTests

# Render PORT expose
EXPOSE 8080

# App run command
CMD ["java", "-jar", "target/*.jar"]