package com.bajaj.quiz.client;

import com.bajaj.quiz.model.PollResponse;
import com.bajaj.quiz.model.SubmitRequest;
import com.bajaj.quiz.model.SubmitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for all quiz API interactions.
 *
 * Responsibilities:
 * - GET  /quiz/messages  — poll for quiz events
 * - POST /quiz/submit    — submit final leaderboard
 *
 * Includes retry logic: if a poll call fails, it waits 2 seconds
 * and retries up to 3 times before giving up.
 */
@Component
public class QuizApiClient {

    private static final Logger log = LoggerFactory.getLogger(QuizApiClient.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final RestTemplate restTemplate;

    @Value("${quiz.base-url}")
    private String baseUrl;

    @Value("${quiz.reg-no}")
    private String regNo;

    public QuizApiClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Polls the quiz API for events at the given poll index.
     * Retries up to 3 times on failure with a 2-second backoff.
     *
     * @param pollIndex  the poll index (0–9)
     * @return PollResponse containing the list of quiz events
     */
    public PollResponse poll(int pollIndex) {
        String url = String.format("%s/quiz/messages?regNo=%s&poll=%d", baseUrl, regNo, pollIndex);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Polling API: poll={} (attempt {}/{})", pollIndex, attempt, MAX_RETRIES);
                ResponseEntity<PollResponse> response = restTemplate.getForEntity(url, PollResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    PollResponse body = response.getBody();
                    int eventCount = (body.getEvents() != null) ? body.getEvents().size() : 0;
                    log.info("Poll {} response received. Events count: {}", pollIndex, eventCount);
                    return body;
                } else {
                    log.warn("Poll {} returned non-success status: {}", pollIndex, response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Poll {} failed on attempt {}: {}", pollIndex, attempt, e.getMessage());
            }

            // Wait before retrying (unless it's the last attempt)
            if (attempt < MAX_RETRIES) {
                try {
                    log.info("Retrying in {}ms...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
            }
        }

        log.error("All {} retries exhausted for poll={}. Returning null.", MAX_RETRIES, pollIndex);
        return null;
    }

    /**
     * Submits the final leaderboard to the quiz API.
     *
     * @param request  the submit request containing regNo and leaderboard
     * @return SubmitResponse with validation results (isCorrect, isIdempotent)
     */
    public SubmitResponse submit(SubmitRequest request) {
        String url = baseUrl + "/quiz/submit";
        log.info("Submitting leaderboard to: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SubmitRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<SubmitResponse> response = restTemplate.postForEntity(url, entity, SubmitResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Submit returned non-success status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Submit failed: {}", e.getMessage());
            return null;
        }
    }
}
