package com.example.online_exam.dto;

import java.util.Map;

/**
 * DTO used when a student submits their exam answers.
 * Maps question ID -> selected answer (A/B/C/D or null for skipped).
 */
public class ExamSubmissionDto {

    private Long attemptId;

    /** Map of questionId -> selected answer ('A','B','C','D') */
    private Map<Long, String> answers;

    public ExamSubmissionDto() {}

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public Map<Long, String> getAnswers() { return answers; }
    public void setAnswers(Map<Long, String> answers) { this.answers = answers; }
}
