package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for the GET /quiz/messages poll API.
 *
 * Contains metadata (regNo, setId, pollIndex) and a list of
 * quiz events for that particular poll index.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollResponse {

    private String regNo;
    private String setId;
    private int pollIndex;
    private List<QuizEvent> events;
}
