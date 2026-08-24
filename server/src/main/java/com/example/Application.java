package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        // Ensure the local SQLite database can be created safely on startup (its parent
        // directory exists) before any connection is opened. This never touches existing data;
        // the schema is applied idempotently by Spring Boot's SQL init.
        application.addInitializers(new DatabaseBootstrap());
        application.run(args);
    }
}
