# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build, lint, and test using Maven:

```bash
# Compile the project
./mvnw compile

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest="com.learn.spring_jpa.SpringJpaApplicationTests"

# Run specific test method (requires surefire plugin)
./mvnw test -Dtest="com.learn.spring_jpa.SpringJpaApplicationTests#contextLoads"

# Generate test reports
./mvnw test -Dtest="com.learn.spring_jpa.SpringJpaApplicationTests" -DfailIfNoTests=false

# Clean and rebuild
./mvnw clean compile
```

## Architecture

This is a Spring Boot 4.1.1 application with the following architecture:

- **Main entry point**: `src/main/java/com/learn/dev-bot`
  - Annotated with `@SpringBootApplication`, serves as the application startup class
  
- **Database layer**: Spring Data JPA with MySQL
  - Database configuration in `src/main/resources/application.properties`
  - URL: `jdbc:mysql://localhost:3306/spring-jpa`
  - User: `root`, no password set
  
- **Web layer**: Spring MVC with security
  - `spring-boot-starter-webmvc` for REST/web endpoints
  - `spring-boot-starter-security` for authentication/authorization
  
- **Testing**: JUnit 5 with Spring Boot test support
  - Test class: `src/test/java/com/learn/spring_jpa/SpringJpaApplicationTests.java`
  - Contains a basic `contextLoads()` test that verifies the application context loads
  - Test dependencies include `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`, and `spring-boot-starter-webmvc-test`

- **Key dependencies** (from `pom.xml`):
  - `spring-boot-starter-data-jpa` - JPA support
  - `spring-boot-starter-security` - Security features
  - `spring-boot-starter-webmvc` - Web MVC
  - `mysql-connector-j` - MySQL driver
  - `lombok` - Boilerplate code reduction
  - `spring-boot-devtools` - Development tools

- **Docker**: `docker-compose.yml` is available for containerized setup

## Code Conventions

- Package structure: `com.learn.spring_jpa`
- Source files under `src/main/java/` and `src/test/java/`
- Use Lombok annotations (`@Data`, `@Entity`, etc.) where appropriate
- Follow Spring Boot conventions for repository interfaces, services, and controllers
- Tests use `@SpringBootTest` annotation for integration testing