package com.bajaj.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bajaj Finserv Health — Quiz Leaderboard Application
 *
 * Spring Boot entry point. The actual quiz logic is triggered
 * automatically via {@link com.bajaj.quiz.runner.QuizRunner}.
 */
@SpringBootApplication
public class QuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizApplication.class, args);
    }
}
