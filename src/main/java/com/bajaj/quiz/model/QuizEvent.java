package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single quiz score event returned by the poll API.
 *
 * Each event contains:
 * - roundId   : identifies the quiz round
 * - participant: name of the participant
 * - score     : points earned in that round
 *
 * Deduplication key = roundId + "_" + participant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizEvent {

    private String roundId;
    private String participant;
    private int score;
}
