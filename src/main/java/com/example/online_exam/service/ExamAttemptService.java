package com.example.online_exam.service;

import com.example.online_exam.dto.ExamSubmissionDto;
import com.example.online_exam.dto.StudentResultDto;
import com.example.online_exam.entity.*;
import com.example.online_exam.exception.ResourceNotFoundException;
import com.example.online_exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamAttemptService {

    private final ExamAttemptRepository attemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    public ExamAttemptService(ExamAttemptRepository attemptRepository,
                               ExamQuestionRepository examQuestionRepository,
                               StudentAnswerRepository studentAnswerRepository,
                               ExamRepository examRepository,
                               UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    /**
     * Starts (or resumes) an exam attempt.
     *
     * <p>Server-side availability is checked before opening the attempt:
     * <ul>
     *   <li>Exam must be published (active=true)</li>
     *   <li>Exam must not be manually closed</li>
     *   <li>Computed status must be {@code AVAILABLE}</li>
     *   <li>Student must not already have a completed attempt</li>
     * </ul>
     */
    @Transactional
    public ExamAttempt startAttempt(Long examId, Long studentId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        // Availability gate
        ExamStatus status = ExamService.computeStatus(exam);
        if (status != ExamStatus.AVAILABLE) {
            throw new IllegalStateException(switch (status) {
                case UNPUBLISHED -> "This exam is not published.";
                case UPCOMING    -> "This exam has not opened yet.";
                case EXPIRED     -> "This exam has expired and is no longer accepting attempts.";
                case CLOSED      -> "This exam has been closed by the administrator.";
                default          -> "This exam is not currently available.";
            });
        }

        // Duplicate attempt gate
        if (attemptRepository.existsByExamIdAndStudentIdAndCompletedTrue(examId, studentId)) {
            throw new IllegalStateException("You have already completed this exam.");
        }

        // Return existing in-progress attempt if within deadline, otherwise reject
        return attemptRepository
                .findByExamIdAndStudentIdAndCompletedFalse(examId, studentId)
                .map(existingAttempt -> {
                    // Check if the student's personal deadline has passed
                    if (existingAttempt.getAttemptDeadline() != null
                            && LocalDateTime.now().isAfter(existingAttempt.getAttemptDeadline().plusMinutes(2))) {
                        // Auto-submit expired in-progress attempt with no answers
                        autoSubmitExpiredAttempt(existingAttempt);
                        return existingAttempt; // Return it so controller can handle the completed state
                    }
                    return existingAttempt;
                })
                .orElseGet(() -> {
                    User student = userRepository.findById(studentId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

                    ExamAttempt attempt = new ExamAttempt(exam, student);

                    // Clamp attempt deadline to exam's validUntil for SCHEDULED_PERIOD exams
                    if (exam.getAvailabilityMode() == ExamAvailabilityMode.SCHEDULED_PERIOD
                            && exam.getValidUntil() != null
                            && attempt.getAttemptDeadline().isAfter(exam.getValidUntil())) {
                        attempt.setAttemptDeadline(exam.getValidUntil());
                    }

                    return attemptRepository.save(attempt);
                });
    }

    /**
     * Auto-submits an in-progress attempt with no answers when the deadline has passed.
     */
    private void autoSubmitExpiredAttempt(ExamAttempt attempt) {
        if (attempt.isCompleted()) return;

        List<ExamQuestion> examQuestions =
                examQuestionRepository.findByExamIdOrderByQuestionOrder(attempt.getExam().getId());

        for (ExamQuestion eq : examQuestions) {
            StudentAnswer sa = studentAnswerRepository
                    .findByAttemptIdAndQuestionId(attempt.getId(), eq.getQuestion().getId())
                    .orElseGet(() -> new StudentAnswer(attempt, eq.getQuestion(), null));
            sa.setSelectedAnswer(null);
            sa.setCorrect(false);
            sa.setMarksAwarded(0.0);
            studentAnswerRepository.save(sa);
        }

        attempt.setScoreObtained(0.0);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setCompleted(true);
        attempt.setPassed(false);
        attemptRepository.save(attempt);
    }

    /**
     * Submits student answers.
     *
     * <p>Server-side deadline enforcement:
     * <ul>
     *   <li>Attempt deadline: {@code attemptDeadline + 2 min grace}</li>
     *   <li>Exam closing time (SCHEDULED_PERIOD): {@code validUntil + 2 min grace}</li>
     * </ul>
     * Whichever is earlier determines the effective deadline. If the current time
     * exceeds the effective deadline, answers are ignored and the attempt receives zero.
     */
    public ExamAttempt submitAttempt(ExamSubmissionDto submission, Long studentId) {
        ExamAttempt attempt = attemptRepository.findById(submission.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", submission.getAttemptId()));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new SecurityException("You are not authorized to submit this attempt.");
        }
        if (attempt.isCompleted()) {
            throw new IllegalStateException("This attempt has already been submitted.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveDeadline = effectiveDeadline(attempt);
        boolean pastDeadline = now.isAfter(effectiveDeadline);

        Map<Long, String> answers = pastDeadline ? null : submission.getAnswers();

        List<ExamQuestion> examQuestions =
                examQuestionRepository.findByExamIdOrderByQuestionOrder(attempt.getExam().getId());

        double totalScore = 0.0;
        for (ExamQuestion eq : examQuestions) {
            Question question = eq.getQuestion();
            String selected = (answers != null) ? answers.get(question.getId()) : null;

            StudentAnswer studentAnswer = studentAnswerRepository
                    .findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
                    .orElseGet(() -> new StudentAnswer(attempt, question, null));
            studentAnswer.setSelectedAnswer(selected);
            boolean isCorrect = selected != null && selected.equals(question.getCorrectAnswer());
            studentAnswer.setCorrect(isCorrect);
            studentAnswer.setMarksAwarded(isCorrect ? question.getPositiveMarks() : 0.0);
            totalScore += studentAnswer.getMarksAwarded();
            studentAnswerRepository.save(studentAnswer);
        }

        attempt.setScoreObtained(totalScore);
        attempt.setSubmittedAt(now);
        attempt.setCompleted(true);

        double percentage = (attempt.getExam().getTotalMarks() > 0)
                ? (totalScore / attempt.getExam().getTotalMarks()) * 100.0
                : 0.0;
        attempt.setPassed(percentage >= attempt.getExam().getPassingPercentage());

        return attemptRepository.save(attempt);
    }

    /**
     * Computes the effective submission deadline (earlier of attempt deadline
     * and exam validUntil), plus 2-minute network grace period.
     */
    public static LocalDateTime effectiveDeadline(ExamAttempt attempt) {
        LocalDateTime deadline = attempt.getAttemptDeadline();
        if (deadline == null) {
            // Legacy attempt without stored deadline – fall back to startedAt + duration
            deadline = attempt.getStartedAt()
                    .plusMinutes(attempt.getExam().getDurationMinutes());
        }

        Exam exam = attempt.getExam();
        if (exam.getAvailabilityMode() == ExamAvailabilityMode.SCHEDULED_PERIOD
                && exam.getValidUntil() != null
                && exam.getValidUntil().isBefore(deadline)) {
            deadline = exam.getValidUntil();
        }

        return deadline.plusMinutes(2); // 2-minute grace period
    }

    // -------------------------------------------------------------------------
    // Results & Reporting
    // -------------------------------------------------------------------------

    /**
     * Maps an ExamAttempt and its answers to a comprehensive StudentResultDto.
     */
    public StudentResultDto mapToResultDto(ExamAttempt attempt, List<StudentAnswer> answers) {
        StudentResultDto dto = new StudentResultDto();
        dto.setResultId(attempt.getId());

        // Actual student identity from StudentProfile or User
        User student = attempt.getStudent();
        StudentProfile profile = (student != null) ? student.getStudentProfile() : null;

        if (profile != null) {
            dto.setStudentName((profile.getFullName() != null && !profile.getFullName().isBlank())
                    ? profile.getFullName()
                    : (student != null ? student.getUsername() : "Unknown Student"));
            dto.setStudentId((profile.getStudentId() != null && !profile.getStudentId().isBlank())
                    ? profile.getStudentId()
                    : (student != null ? String.valueOf(student.getId()) : "N/A"));
            dto.setStudentEmail((profile.getEmail() != null && !profile.getEmail().isBlank())
                    ? profile.getEmail()
                    : (student != null ? (student.getUsername() + "@exam.local") : "N/A"));
        } else if (student != null) {
            dto.setStudentName(student.getUsername());
            dto.setStudentId("STD-" + student.getId());
            dto.setStudentEmail(student.getUsername() + "@exam.local");
        } else {
            dto.setStudentName("Unknown Student");
            dto.setStudentId("N/A");
            dto.setStudentEmail("N/A");
        }

        // Exam & Subject details
        Exam exam = attempt.getExam();
        if (exam != null) {
            dto.setExamId(exam.getId());
            dto.setExamTitle(exam.getTitle());
            if (exam.getSubject() != null) {
                dto.setSubjectId(exam.getSubject().getId());
                dto.setSubjectName(exam.getSubject().getName());
            } else {
                dto.setSubjectName("N/A");
            }
            dto.setTotalMarks(exam.getTotalMarks());
        } else {
            dto.setExamTitle("N/A");
            dto.setSubjectName("N/A");
            dto.setTotalMarks(0.0);
        }

        dto.setSubmittedAt(attempt.getSubmittedAt());
        dto.setMarksObtained(attempt.getScoreObtained());

        double percentage = (dto.getTotalMarks() > 0)
                ? (dto.getMarksObtained() / dto.getTotalMarks()) * 100.0
                : 0.0;
        dto.setPercentage(Math.round(percentage * 100.0) / 100.0);

        if (attempt.getPassed() != null) {
            dto.setPassed(attempt.getPassed());
        } else if (exam != null) {
            dto.setPassed(percentage >= exam.getPassingPercentage());
        } else {
            dto.setPassed(false);
        }

        // Questions & answers evaluation
        int totalQuestions = (answers != null && !answers.isEmpty())
                ? answers.size()
                : (exam != null && exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0);
        dto.setTotalQuestions(totalQuestions);

        int attemptedCount = 0;
        int correctCount = 0;
        if (answers != null) {
            for (StudentAnswer ans : answers) {
                if (ans.getSelectedAnswer() != null && !ans.getSelectedAnswer().trim().isEmpty()) {
                    attemptedCount++;
                }
                if (Boolean.TRUE.equals(ans.getCorrect())) {
                    correctCount++;
                }
            }
        }
        dto.setAttemptedQuestions(attemptedCount);
        dto.setCorrectAnswers(correctCount);
        dto.setWrongAnswers(Math.max(0, attemptedCount - correctCount));

        return dto;
    }

    /**
     * Retrieves all completed exam results with optional filters and sorting.
     */
    @Transactional(readOnly = true)
    public List<StudentResultDto> findFilteredResults(String studentName,
                                                      String studentId,
                                                      String email,
                                                      Long examId,
                                                      Long subjectId,
                                                      String status,
                                                      String sortBy) {
        List<ExamAttempt> attempts = attemptRepository.findAllCompleted();
        if (attempts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> attemptIds = attempts.stream().map(ExamAttempt::getId).toList();
        List<StudentAnswer> allAnswers = studentAnswerRepository.findByAttemptIdIn(attemptIds);
        Map<Long, List<StudentAnswer>> answersByAttempt = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getAttempt().getId()));

        List<StudentResultDto> dtos = attempts.stream()
                .map(att -> mapToResultDto(att, answersByAttempt.getOrDefault(att.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        // Apply filters
        return dtos.stream()
                .filter(r -> {
                    if (studentName != null && !studentName.isBlank()) {
                        if (r.getStudentName() == null ||
                                !r.getStudentName().toLowerCase().contains(studentName.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (studentId != null && !studentId.isBlank()) {
                        if (r.getStudentId() == null ||
                                !r.getStudentId().toLowerCase().contains(studentId.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (email != null && !email.isBlank()) {
                        if (r.getStudentEmail() == null ||
                                !r.getStudentEmail().toLowerCase().contains(email.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (examId != null) {
                        if (r.getExamId() == null || !r.getExamId().equals(examId)) {
                            return false;
                        }
                    }
                    if (subjectId != null) {
                        if (r.getSubjectId() == null || !r.getSubjectId().equals(subjectId)) {
                            return false;
                        }
                    }
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        if ("PASS".equalsIgnoreCase(status) && !r.isPassed()) {
                            return false;
                        }
                        if ("FAIL".equalsIgnoreCase(status) && r.isPassed()) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(getComparator(sortBy))
                .collect(Collectors.toList());
    }

    private Comparator<StudentResultDto> getComparator(String sortBy) {
        if (sortBy == null) sortBy = "date_desc";
        return switch (sortBy.toLowerCase()) {
            case "date_asc" -> Comparator.comparing(StudentResultDto::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "score_desc" -> Comparator.comparingDouble(StudentResultDto::getMarksObtained).reversed();
            case "score_asc" -> Comparator.comparingDouble(StudentResultDto::getMarksObtained);
            case "percentage_desc" -> Comparator.comparingDouble(StudentResultDto::getPercentage).reversed();
            case "percentage_asc" -> Comparator.comparingDouble(StudentResultDto::getPercentage);
            case "date_desc" -> Comparator.comparing(StudentResultDto::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(StudentResultDto::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ExamAttempt findById(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttempt", id));
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> findByStudent(Long studentId) {
        return attemptRepository.findByStudentIdAndCompletedTrueOrderBySubmittedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> findByExam(Long examId) {
        return attemptRepository.findByExamId(examId);
    }

    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersForAttempt(Long attemptId) {
        return studentAnswerRepository.findByAttemptId(attemptId);
    }

    @Transactional(readOnly = true)
    public long countAllCompleted() {
        return attemptRepository.countByCompletedTrue();
    }
}
