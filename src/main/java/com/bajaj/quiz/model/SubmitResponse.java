package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for POST /quiz/submit.
 *
 * Actual API response format:
 * {
 *   "regNo": "RA2311003011748",
 *   "totalPollsMade": 38,
 *   "submittedTotal": 2290,
 *   "attemptCount": 5
 * }
 *
 * Note: May also include isCorrect, isIdempotent, expectedTotal, message
 * depending on the API version. All unknown fields are safely ignored.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitResponse {

    private String regNo;
    private int totalPollsMade;
    private int submittedTotal;
    private int attemptCount;
    private int expectedTotal;
    private String message;

    // These may or may not be present depending on API version
    @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
    private Boolean correct;

    @com.fasterxml.jackson.annotation.JsonProperty("isIdempotent")
    private Boolean idempotent;
}
