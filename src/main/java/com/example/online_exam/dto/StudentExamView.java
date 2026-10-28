package com.example.online_exam.dto;

import com.example.online_exam.entity.Exam;
import com.example.online_exam.entity.ExamAttempt;
import com.example.online_exam.entity.ExamStatus;

/**
 * A view of a single exam as seen by a student on their dashboard.
 * Combines the exam metadata, its computed status, and any attempt the
 * student has already made.
 */
public class StudentExamView {

    private final Exam exam;
    private final ExamStatus status;
    /** May be null if the student has not attempted this exam. */
    private final ExamAttempt attempt;

    public StudentExamView(Exam exam, ExamStatus status, ExamAttempt attempt) {
        this.exam = exam;
        this.status = status;
        this.attempt = attempt;
    }

    public Exam getExam() { return exam; }
    public ExamStatus getStatus() { return status; }
    public ExamAttempt getAttempt() { return attempt; }

    public boolean isAttempted() { return attempt != null && attempt.isCompleted(); }
    public boolean isInProgress() { return attempt != null && !attempt.isCompleted(); }
}
