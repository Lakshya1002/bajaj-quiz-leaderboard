package com.bajaj.quiz;

import com.bajaj.quiz.model.LeaderboardEntry;
import com.bajaj.quiz.model.QuizEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the core deduplication and aggregation logic.
 *
 * These tests validate the business rules WITHOUT requiring
 * Spring context or network calls — pure logic tests.
 */
class DeduplicationTest {

    // ──────────────────────────────────────────
    //  Helper: replicates the service dedup logic
    // ──────────────────────────────────────────

    private List<LeaderboardEntry> processEvents(List<QuizEvent> events) {
        Set<String> seenKeys = new HashSet<>();
        Map<String, Integer> scoreMap = new HashMap<>();

        for (QuizEvent event : events) {
            String dedupKey = event.getRoundId() + "_" + event.getParticipant();

            if (!seenKeys.contains(dedupKey)) {
                seenKeys.add(dedupKey);
                scoreMap.merge(event.getParticipant(), event.getScore(), Integer::sum);
            }
        }

        return scoreMap.entrySet().stream()
                .map(e -> new LeaderboardEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed())
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    //  TEST 1: Basic aggregation (no duplicates)
    // ──────────────────────────────────────────

    @Test
    @DisplayName("Basic aggregation with no duplicate events")
    void testNoDuplicates_basicAggregation() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R2", "Alice", 20),
                new QuizEvent("R1", "Bob", 15)
        );

        List<LeaderboardEntry> result = processEvents(events);

        assertEquals(2, result.size());

        // Alice: 10 + 20 = 30
        LeaderboardEntry alice = result.stream()
                .filter(e -> e.getParticipant().equals("Alice"))
                .findFirst().orElseThrow();
        assertEquals(30, alice.getTotalScore());

        // Bob: 15
        LeaderboardEntry bob = result.stream()
                .filter(e -> e.getParticipant().equals("Bob"))
                .findFirst().orElseThrow();
        assertEquals(15, bob.getTotalScore());
    }

    // ──────────────────────────────────────────
    //  TEST 2: Duplicate events are ignored
    // ──────────────────────────────────────────

    @Test
    @DisplayName("Duplicate events (same roundId + participant) are ignored")
    void testDuplicateEventsIgnored() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R1", "Alice", 10),   // exact duplicate
                new QuizEvent("R1", "Alice", 10),   // another duplicate
                new QuizEvent("R2", "Alice", 20)
        );

        List<LeaderboardEntry> result = processEvents(events);

        // Alice should have 10 + 20 = 30 (NOT 10+10+10+20 = 50)
        LeaderboardEntry alice = result.stream()
                .filter(e -> e.getParticipant().equals("Alice"))
                .findFirst().orElseThrow();
        assertEquals(30, alice.getTotalScore());
    }

    // ──────────────────────────────────────────
    //  TEST 3: Leaderboard sorted descending
    // ──────────────────────────────────────────

    @Test
    @DisplayName("Leaderboard is sorted in descending order by totalScore")
    void testSortDescending() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Charlie", 5),
                new QuizEvent("R1", "Alice", 30),
                new QuizEvent("R1", "Bob", 20)
        );

        List<LeaderboardEntry> result = processEvents(events);

        assertEquals("Alice", result.get(0).getParticipant());
        assertEquals("Bob", result.get(1).getParticipant());
        assertEquals("Charlie", result.get(2).getParticipant());
    }

    // ──────────────────────────────────────────
    //  TEST 4: Total score is correct after dedup
    // ──────────────────────────────────────────

    @Test
    @DisplayName("Total score across all participants is correct after deduplication")
    void testTotalScoreCorrect() {
        List<QuizEvent> events = List.of(
                new QuizEvent("R1", "Alice", 10),
                new QuizEvent("R1", "Bob", 20),
                new QuizEvent("R2", "Alice", 15),
                new QuizEvent("R1", "Alice", 10),   // duplicate — ignored
                new QuizEvent("R1", "Bob", 20),     // duplicate — ignored
                new QuizEvent("R2", "Bob", 25)
        );

        List<LeaderboardEntry> result = processEvents(events);

        // Alice: 10 + 15 = 25
        // Bob:   20 + 25 = 45
        // Total: 70
        int totalScore = result.stream()
                .mapToInt(LeaderboardEntry::getTotalScore)
                .sum();
        assertEquals(70, totalScore);

        // Verify Bob is ranked first (45 > 25)
        assertEquals("Bob", result.get(0).getParticipant());
        assertEquals(45, result.get(0).getTotalScore());
    }
}
