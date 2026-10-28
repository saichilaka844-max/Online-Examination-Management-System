package com.example.online_exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Records a single student's attempt at an exam.
 *
 * <p>The attempt has two independent deadlines that are both enforced server-side:
 * <ol>
 *   <li>{@code attemptDeadline} – the student's personal time limit
 *       ({@code startedAt + exam.durationMinutes}).</li>
 *   <li>The exam's {@code validUntil} (for {@code SCHEDULED_PERIOD} exams).</li>
 * </ol>
 * Submission is rejected if the current time is after the <em>earlier</em> of
 * these two deadlines (plus a 2-minute network grace period).
 */
@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * UTC instant by which the student must submit (startedAt + durationMinutes).
     * Stored so validation does not depend on the browser clock.
     */
    @Column(name = "attempt_deadline")
    private LocalDateTime attemptDeadline;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "score_obtained")
    private double scoreObtained;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StudentAnswer> answers = new ArrayList<>();

    public ExamAttempt() {}

    public ExamAttempt(Exam exam, User student) {
        this.exam = exam;
        this.student = student;
        this.startedAt = LocalDateTime.now();
        this.attemptDeadline = this.startedAt.plusMinutes(exam.getDurationMinutes());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getAttemptDeadline() { return attemptDeadline; }
    public void setAttemptDeadline(LocalDateTime attemptDeadline) { this.attemptDeadline = attemptDeadline; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public double getScoreObtained() { return scoreObtained; }
    public void setScoreObtained(double scoreObtained) { this.scoreObtained = scoreObtained; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public List<StudentAnswer> getAnswers() { return answers; }
    public void setAnswers(List<StudentAnswer> answers) { this.answers = answers; }
}
