package com.example.online_exam.dto;

import com.example.online_exam.entity.ExamAvailabilityMode;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for creating and updating an Exam.
 *
 * <h3>Availability fields</h3>
 * <ul>
 *   <li>{@code availabilityMode} selects the scheduling model.</li>
 *   <li>{@code validFrom} – required for both modes (when to open).</li>
 *   <li>{@code validUntil} – required only when mode is {@code SCHEDULED_PERIOD}.</li>
 * </ul>
 * Service-layer validation enforces these rules because cross-field constraints
 * are not easily expressed with Bean Validation annotations.
 */
public class ExamDto {

    private Long id;

    @NotBlank(message = "Exam title is required")
    @Size(max = 200)
    private String title;

    @NotNull(message = "Subject is required")
    private Long subjectId;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;

    @DecimalMin(value = "0.0", message = "Passing percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Passing percentage cannot exceed 100")
    private double passingPercentage;

    private boolean active;

    // ---- Availability scheduling ----

    private ExamAvailabilityMode availabilityMode = ExamAvailabilityMode.NO_EXPIRY;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime validFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime validUntil;

    // ---- Questions ----

    /** IDs of questions selected from the bank */
    private List<Long> questionIds = new java.util.ArrayList<>();

    @Valid
    private List<InlineQuestionDto> questions = new java.util.ArrayList<>();

    public ExamDto() {}

    // ---- getters / setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getPassingPercentage() { return passingPercentage; }
    public void setPassingPercentage(double passingPercentage) { this.passingPercentage = passingPercentage; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public ExamAvailabilityMode getAvailabilityMode() { return availabilityMode; }
    public void setAvailabilityMode(ExamAvailabilityMode availabilityMode) {
        this.availabilityMode = (availabilityMode != null) ? availabilityMode : ExamAvailabilityMode.NO_EXPIRY;
    }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }

    public List<InlineQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<InlineQuestionDto> questions) { this.questions = questions; }

    public static class InlineQuestionDto {
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
    }
}
