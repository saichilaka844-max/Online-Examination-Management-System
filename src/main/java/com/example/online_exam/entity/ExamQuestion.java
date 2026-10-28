package com.example.online_exam.entity;

import jakarta.persistence.*;

/**
 * Join table between Exam and Question, with an explicit ordering field.
 */
@Entity
@Table(name = "exam_questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "question_id"}))
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order")
    private int questionOrder;

    public ExamQuestion() {}

    public ExamQuestion(Exam exam, Question question, int questionOrder) {
        this.exam = exam;
        this.question = question;
        this.questionOrder = questionOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public int getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }
}
