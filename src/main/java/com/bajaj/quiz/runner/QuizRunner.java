package com.bajaj.quiz.runner;

import com.bajaj.quiz.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Application runner — triggers the quiz pipeline automatically
 * when the Spring Boot application starts.
 *
 * This ensures the quiz logic runs once on startup without
 * requiring any manual HTTP call or endpoint trigger.
 */
@Component
public class QuizRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizRunner.class);

    private final QuizService quizService;

    public QuizRunner(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    public void run(String... args) {
        log.info("QuizRunner started — launching quiz pipeline...");

        long startTime = System.currentTimeMillis();
        quizService.run();
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("QuizRunner finished. Total execution time: {}s", elapsed / 1000);
    }
}
