package com.example.online_exam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an exam created by an admin.
 *
 * <h3>Availability model</h3>
 * <ul>
 *   <li>{@code active} – publication flag; unpublished exams are never visible to students.</li>
 *   <li>{@code manuallyClosed} – admin can forcibly close an otherwise-available exam.</li>
 *   <li>{@code availabilityMode} – {@code NO_EXPIRY} or {@code SCHEDULED_PERIOD}.</li>
 *   <li>{@code validFrom} – the earliest UTC instant the exam opens (both modes).</li>
 *   <li>{@code validUntil} – the UTC instant the exam closes ({@code SCHEDULED_PERIOD} only).</li>
 * </ul>
 *
 * <h3>Defaults for existing records (schema migration)</h3>
 * Hibernate {@code ddl-auto=update} adds the new columns as nullable.  Existing
 * rows will have NULL values; the service layer treats NULL as:
 * <ul>
 *   <li>{@code availabilityMode} NULL → {@code NO_EXPIRY}</li>
 *   <li>{@code validFrom} NULL → epoch (always open)</li>
 *   <li>{@code validUntil} NULL → no end date</li>
 *   <li>{@code manuallyClosed} NULL / false → not closed</li>
 * </ul>
 */
@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Exam title is required")
    @Size(max = 200, message = "Exam title must not exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "total_marks", nullable = false)
    private double totalMarks;

    @DecimalMin(value = "0.0", message = "Passing percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Passing percentage cannot exceed 100")
    @Column(name = "passing_percentage", nullable = false)
    private double passingPercentage;

    /** Publication flag – false means the exam is invisible to students. */
    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    /**
     * When true an admin has manually closed this exam regardless of
     * scheduled dates or the availability mode.
     */
    @Column(name = "manually_closed", nullable = false)
    private boolean manuallyClosed = false;

    /**
     * Availability window mode.
     * Default {@code NO_EXPIRY}; stored as a VARCHAR so the column is
     * backward-compatible when added to an existing table.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_mode", nullable = false, length = 20)
    private ExamAvailabilityMode availabilityMode = ExamAvailabilityMode.NO_EXPIRY;

    /**
     * UTC timestamp from which the exam becomes available.
     * For {@code NO_EXPIRY} exams this is the opening time.
     * May be null for existing records (treated as "always open").
     */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /**
     * UTC timestamp at which the exam closes.
     * Only meaningful for {@code SCHEDULED_PERIOD} mode.
     * Null for {@code NO_EXPIRY} exams.
     */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExamAttempt> attempts = new ArrayList<>();

    public Exam() {}

    public Exam(String title, Subject subject, int durationMinutes,
                double totalMarks, double passingPercentage, boolean active) {
        this.title = title;
        this.subject = subject;
        this.durationMinutes = durationMinutes;
        this.totalMarks = totalMarks;
        this.passingPercentage = passingPercentage;
        this.active = active;
    }

    // ---- getters / setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(double totalMarks) { this.totalMarks = totalMarks; }

    public double getPassingPercentage() { return passingPercentage; }
    public void setPassingPercentage(double passingPercentage) { this.passingPercentage = passingPercentage; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isManuallyClosed() { return manuallyClosed; }
    public void setManuallyClosed(boolean manuallyClosed) { this.manuallyClosed = manuallyClosed; }

    public ExamAvailabilityMode getAvailabilityMode() { return availabilityMode; }
    public void setAvailabilityMode(ExamAvailabilityMode availabilityMode) {
        this.availabilityMode = availabilityMode;
    }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public List<ExamQuestion> getExamQuestions() { return examQuestions; }
    public void setExamQuestions(List<ExamQuestion> examQuestions) { this.examQuestions = examQuestions; }

    public List<ExamAttempt> getAttempts() { return attempts; }
    public void setAttempts(List<ExamAttempt> attempts) { this.attempts = attempts; }
}
