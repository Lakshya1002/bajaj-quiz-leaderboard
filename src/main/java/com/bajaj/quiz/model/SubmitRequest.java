package com.bajaj.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for POST /quiz/submit.
 * Contains the registration number and the computed leaderboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequest {

    private String regNo;
    private List<LeaderboardEntry> leaderboard;
}
