package com.bajaj.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single entry in the final leaderboard.
 * Contains a participant's name and their aggregated total score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private String participant;
    private int totalScore;
}
