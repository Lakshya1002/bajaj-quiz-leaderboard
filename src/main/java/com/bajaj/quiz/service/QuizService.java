package com.bajaj.quiz.service;

import com.bajaj.quiz.client.QuizApiClient;
import com.bajaj.quiz.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core quiz service — the brain of the application.
 *
 * Orchestrates the full pipeline:
 * 1. Poll the API 10 times (with 5s delay between each)
 * 2. Deduplicate events using (roundId + "_" + participant) as the unique key
 * 3. Aggregate scores per participant
 * 4. Sort leaderboard descending by totalScore
 * 5. Submit the leaderboard exactly once (idempotent guard)
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizApiClient apiClient;

    @Value("${quiz.reg-no}")
    private String regNo;

    @Value("${quiz.poll-delay-ms}")
    private long pollDelayMs;

    @Value("${quiz.total-polls}")
    private int totalPolls;

    /** Guard flag to ensure submit is called exactly once */
    private boolean hasSubmitted = false;

    public QuizService(QuizApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // ──────────────────────────────────────────────
    //  PUBLIC ENTRY POINT
    // ──────────────────────────────────────────────

    /**
     * Runs the full quiz pipeline: collect → dedup → aggregate → sort → submit.
     */
    public void run() {
        log.info("═══════════════════════════════════════════════");
        log.info("  Bajaj Quiz Leaderboard — Starting Pipeline  ");
        log.info("═══════════════════════════════════════════════");
        log.info("Registration No : {}", regNo);
        log.info("Total Polls     : {}", totalPolls);
        log.info("Poll Delay      : {}ms", pollDelayMs);
        log.info("───────────────────────────────────────────────");

        // Step 1 — Collect all events from 10 polls
        List<QuizEvent> allEvents = collectAllEvents();
        log.info("All polls complete. Total raw events collected: {}", allEvents.size());

        if (allEvents.isEmpty()) {
            log.error("No events collected. Cannot proceed. Exiting.");
            return;
        }

        // Step 2 & 3 — Deduplicate and aggregate scores
        Map<String, Integer> scoreMap = deduplicateAndAggregate(allEvents);

        // Step 4 — Build sorted leaderboard
        List<LeaderboardEntry> leaderboard = buildLeaderboard(scoreMap);

        // Step 5 — Submit
        submitLeaderboard(leaderboard);
    }

    // ──────────────────────────────────────────────
    //  STEP 1: COLLECT ALL EVENTS
    // ──────────────────────────────────────────────

    private List<QuizEvent> collectAllEvents() {
        List<QuizEvent> allEvents = new ArrayList<>();

        for (int i = 0; i < totalPolls; i++) {
            PollResponse response = apiClient.poll(i);

            if (response != null && response.getEvents() != null) {
                allEvents.addAll(response.getEvents());
            } else {
                log.warn("Poll {} returned no events or failed entirely.", i);
            }

            // Mandatory 5-second delay between polls (skip after last poll)
            if (i < totalPolls - 1) {
                try {
                    log.debug("Waiting {}ms before next poll...", pollDelayMs);
                    Thread.sleep(pollDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted during poll delay. Aborting.");
                    break;
                }
            }
        }

        return allEvents;
    }

    // ──────────────────────────────────────────────
    //  STEP 2 & 3: DEDUPLICATE + AGGREGATE
    // ──────────────────────────────────────────────

    private Map<String, Integer> deduplicateAndAggregate(List<QuizEvent> events) {
        Set<String> seenKeys = new HashSet<>();
        Map<String, Integer> scoreMap = new HashMap<>();
        int duplicateCount = 0;

        for (QuizEvent event : events) {
            // Build the unique deduplication key
            String dedupKey = event.getRoundId() + "_" + event.getParticipant();

            if (seenKeys.contains(dedupKey)) {
                // Duplicate — skip this event
                log.debug("Duplicate skipped: {}", dedupKey);
                duplicateCount++;
            } else {
                // New unique event — record it
                seenKeys.add(dedupKey);
                scoreMap.merge(event.getParticipant(), event.getScore(), Integer::sum);
                log.debug("Accepted: {} | Score: {}", dedupKey, event.getScore());
            }
        }

        log.info("───────────────────────────────────────────────");
        log.info("Deduplication complete. Duplicates skipped: {} | Unique entries: {}",
                duplicateCount, seenKeys.size());

        return scoreMap;
    }

    // ──────────────────────────────────────────────
    //  STEP 4: BUILD SORTED LEADERBOARD
    // ──────────────────────────────────────────────

    private List<LeaderboardEntry> buildLeaderboard(Map<String, Integer> scoreMap) {
        List<LeaderboardEntry> leaderboard = scoreMap.entrySet().stream()
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed())
                .collect(Collectors.toList());

        log.info("───────────────────────────────────────────────");
        log.info("  FINAL LEADERBOARD ({} participants)", leaderboard.size());
        log.info("───────────────────────────────────────────────");

        int rank = 1;
        for (LeaderboardEntry entry : leaderboard) {
            log.info("  Rank {} | {} | Score: {}",
                    String.format("%2d", rank++),
                    String.format("%-20s", entry.getParticipant()),
                    entry.getTotalScore());
        }

        log.info("───────────────────────────────────────────────");
        return leaderboard;
    }

    // ──────────────────────────────────────────────
    //  STEP 5: SUBMIT (EXACTLY ONCE)
    // ──────────────────────────────────────────────

    private void submitLeaderboard(List<LeaderboardEntry> leaderboard) {
        // Idempotent guard — never submit more than once
        if (hasSubmitted) {
            log.warn("Leaderboard already submitted. Skipping duplicate submission.");
            return;
        }

        hasSubmitted = true;

        SubmitRequest request = new SubmitRequest(regNo, leaderboard);
        log.info("Submitting leaderboard with {} entries for regNo: {}",
                leaderboard.size(), regNo);

        SubmitResponse response = apiClient.submit(request);

        if (response != null) {
            log.info("═══════════════════════════════════════════════");
            log.info("  SUBMISSION RESULT                            ");
            log.info("═══════════════════════════════════════════════");
            log.info("  regNo          : {}", response.getRegNo());
            log.info("  submittedTotal : {}", response.getSubmittedTotal());
            log.info("  totalPollsMade : {}", response.getTotalPollsMade());
            log.info("  attemptCount   : {}", response.getAttemptCount());

            // Show optional fields if present
            if (response.getCorrect() != null) {
                log.info("  isCorrect      : {}", response.getCorrect());
            }
            if (response.getIdempotent() != null) {
                log.info("  isIdempotent   : {}", response.getIdempotent());
            }
            if (response.getExpectedTotal() != 0) {
                log.info("  expectedTotal  : {}", response.getExpectedTotal());
            }
            if (response.getMessage() != null) {
                log.info("  message        : {}", response.getMessage());
            }

            log.info("═══════════════════════════════════════════════");

            if (response.getAttemptCount() == 1) {
                log.info("✅ SUCCESS — First submission accepted! (attemptCount = 1)");
            } else {
                log.warn("⚠️  Multiple submissions detected (attemptCount = {}). Idempotency check may fail.",
                        response.getAttemptCount());
            }

            // Verify correctness if the field is available
            if (response.getCorrect() != null && response.getCorrect()) {
                log.info("✅ isCorrect = true — Leaderboard is valid!");
            } else if (response.getCorrect() != null && !response.getCorrect()) {
                log.error("❌ isCorrect = false — Leaderboard was NOT correct.");
            }

            log.info("✅ Leaderboard submitted successfully. Total score: {}", response.getSubmittedTotal());
        } else {
            log.error("Submit returned null response. Submission may have failed.");
        }
    }
}
