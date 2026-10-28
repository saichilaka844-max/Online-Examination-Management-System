package com.example.online_exam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question text is required")
    @Size(max = 2000, message = "Question text must not exceed 2000 characters")
    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @NotBlank(message = "Option A is required")
    @Size(max = 500)
    @Column(name = "option_a", nullable = false, length = 500)
    private String optionA;

    @NotBlank(message = "Option B is required")
    @Size(max = 500)
    @Column(name = "option_b", nullable = false, length = 500)
    private String optionB;

    @NotBlank(message = "Option C is required")
    @Size(max = 500)
    @Column(name = "option_c", nullable = false, length = 500)
    private String optionC;

    @NotBlank(message = "Option D is required")
    @Size(max = 500)
    @Column(name = "option_d", nullable = false, length = 500)
    private String optionD;

    /**
     * Correct answer stored as 'A', 'B', 'C', or 'D'.
     */
    @NotBlank(message = "Correct answer is required")
    @Pattern(regexp = "[ABCD]", message = "Correct answer must be A, B, C, or D")
    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer;

    @DecimalMin(value = "0.0", inclusive = false, message = "Positive marks must be greater than 0")
    @Column(name = "positive_marks", nullable = false)
    private double positiveMarks = 1.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    public Question() {}

    public Question(String questionText, String optionA, String optionB,
                    String optionC, String optionD, String correctAnswer,
                    double positiveMarks, Subject subject) {
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.positiveMarks = positiveMarks;
        this.subject = subject;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public double getPositiveMarks() { return positiveMarks; }
    public void setPositiveMarks(double positiveMarks) { this.positiveMarks = positiveMarks; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public List<ExamQuestion> getExamQuestions() { return examQuestions; }
    public void setExamQuestions(List<ExamQuestion> examQuestions) { this.examQuestions = examQuestions; }
}
