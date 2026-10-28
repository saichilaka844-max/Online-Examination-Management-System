package com.example.online_exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"}))
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * The answer selected by the student: 'A', 'B', 'C', or 'D'. Null means skipped.
     */
    @Column(name = "selected_answer", length = 1)
    private String selectedAnswer;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "marks_awarded")
    private double marksAwarded;

    public StudentAnswer() {}

    public StudentAnswer(ExamAttempt attempt, Question question, String selectedAnswer) {
        this.attempt = attempt;
        this.question = question;
        this.selectedAnswer = selectedAnswer;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExamAttempt getAttempt() { return attempt; }
    public void setAttempt(ExamAttempt attempt) { this.attempt = attempt; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public double getMarksAwarded() { return marksAwarded; }
    public void setMarksAwarded(double marksAwarded) { this.marksAwarded = marksAwarded; }
}
