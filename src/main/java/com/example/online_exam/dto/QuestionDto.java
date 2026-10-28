package com.example.online_exam.dto;

import jakarta.validation.constraints.*;

public class QuestionDto {

    private Long id;

    @NotBlank(message = "Question text is required")
    @Size(max = 2000)
    private String questionText;

    @NotBlank(message = "Option A is required")
    @Size(max = 500)
    private String optionA;

    @NotBlank(message = "Option B is required")
    @Size(max = 500)
    private String optionB;

    @NotBlank(message = "Option C is required")
    @Size(max = 500)
    private String optionC;

    @NotBlank(message = "Option D is required")
    @Size(max = 500)
    private String optionD;

    @NotBlank(message = "Correct answer is required")
    @Pattern(regexp = "[ABCD]", message = "Correct answer must be A, B, C, or D")
    private String correctAnswer;

    @DecimalMin(value = "0.0", inclusive = false, message = "Positive marks must be greater than 0")
    private double positiveMarks = 1.0;

    @NotNull(message = "Subject is required")
    private Long subjectId;

    public QuestionDto() {}

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

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
}
