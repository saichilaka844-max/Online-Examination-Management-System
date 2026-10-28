package com.example.online_exam.service;

import com.example.online_exam.dto.ExamDto;
import com.example.online_exam.dto.StudentExamView;
import com.example.online_exam.entity.*;
import com.example.online_exam.exception.ResourceNotFoundException;
import com.example.online_exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService {

    private final ExamRepository examRepository;
    private final SubjectService subjectService;
    private final QuestionRepository questionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository attemptRepository;

    public ExamService(ExamRepository examRepository,
                       SubjectService subjectService,
                       QuestionRepository questionRepository,
                       ExamQuestionRepository examQuestionRepository,
                       ExamAttemptRepository attemptRepository) {
        this.examRepository = examRepository;
        this.subjectService = subjectService;
        this.questionRepository = questionRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.attemptRepository = attemptRepository;
    }

    // -------------------------------------------------------------------------
    // Status computation
    // -------------------------------------------------------------------------

    /**
     * Computes the availability status of an exam at the given instant.
     *
     * <pre>
     * UNPUBLISHED  → active == false
     * CLOSED       → manuallyClosed == true  (overrides everything else)
     * UPCOMING     → validFrom != null && validFrom > now
     * AVAILABLE    → within the open window
     * EXPIRED      → SCHEDULED_PERIOD && validUntil != null && validUntil <= now
     * </pre>
     */
    public static ExamStatus computeStatus(Exam exam, LocalDateTime now) {
        if (!exam.isActive()) return ExamStatus.UNPUBLISHED;
        if (exam.isManuallyClosed()) return ExamStatus.CLOSED;

        // Not yet open?
        if (exam.getValidFrom() != null && exam.getValidFrom().isAfter(now)) {
            return ExamStatus.UPCOMING;
        }

        // Scheduled mode: check if the window has closed
        if (exam.getAvailabilityMode() == ExamAvailabilityMode.SCHEDULED_PERIOD
                && exam.getValidUntil() != null
                && !now.isBefore(exam.getValidUntil())) {
            return ExamStatus.EXPIRED;
        }

        return ExamStatus.AVAILABLE;
    }

    /** Convenience overload that uses the current UTC time. */
    public static ExamStatus computeStatus(Exam exam) {
        return computeStatus(exam, LocalDateTime.now());
    }

    /** Returns true only when computeStatus is AVAILABLE. */
    public static boolean isCurrentlyAvailable(Exam exam) {
        return computeStatus(exam) == ExamStatus.AVAILABLE;
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    public Exam createExam(ExamDto dto) {
        validateDto(dto);
        Subject subject = subjectService.findById(dto.getSubjectId());

        List<Question> bankQuestions = resolveQuestions(dto.getQuestionIds());
        List<Question> inlineQuestions = processInlineQuestions(dto, subject);

        List<Question> allQuestions = new ArrayList<>(inlineQuestions);
        for (Question q : bankQuestions) {
            if (!allQuestions.contains(q)) {
                allQuestions.add(q);
            }
        }

        double totalMarks = allQuestions.stream().mapToDouble(Question::getPositiveMarks).sum();

        Exam exam = new Exam(
                dto.getTitle(), subject,
                dto.getDurationMinutes(), totalMarks,
                dto.getPassingPercentage(), dto.isActive());

        applyAvailabilityFields(exam, dto);
        
        for (int i = 0; i < allQuestions.size(); i++) {
            exam.getExamQuestions().add(new ExamQuestion(exam, allQuestions.get(i), i + 1));
        }
        
        return examRepository.save(exam);
    }

    public Exam updateExam(Long id, ExamDto dto) {
        validateDto(dto);
        Exam exam = findById(id);
        Subject subject = subjectService.findById(dto.getSubjectId());

        List<Question> bankQuestions = resolveQuestions(dto.getQuestionIds());
        List<Question> inlineQuestions = processInlineQuestions(dto, subject);

        List<Question> allQuestions = new ArrayList<>(inlineQuestions);
        for (Question q : bankQuestions) {
            if (!allQuestions.contains(q)) {
                allQuestions.add(q);
            }
        }

        double totalMarks = allQuestions.stream().mapToDouble(Question::getPositiveMarks).sum();

        exam.setTitle(dto.getTitle());
        exam.setSubject(subject);
        exam.setDurationMinutes(dto.getDurationMinutes());
        exam.setTotalMarks(totalMarks);
        exam.setPassingPercentage(dto.getPassingPercentage());
        exam.setActive(dto.isActive());

        applyAvailabilityFields(exam, dto);

        synchronizeExamQuestions(exam, allQuestions);

        return examRepository.save(exam);
    }

    private void synchronizeExamQuestions(Exam exam, List<Question> submittedQuestions) {
        List<ExamQuestion> existing = exam.getExamQuestions();
        
        List<Long> submittedIds = submittedQuestions.stream()
                .filter(q -> q.getId() != null)
                .map(Question::getId)
                .collect(Collectors.toList());

        existing.removeIf(eq -> !submittedIds.contains(eq.getQuestion().getId()));

        for (int i = 0; i < submittedQuestions.size(); i++) {
            Question q = submittedQuestions.get(i);
            int order = i + 1;

            ExamQuestion eq = existing.stream()
                    .filter(e -> e.getQuestion().getId().equals(q.getId()))
                    .findFirst()
                    .orElse(null);

            if (eq != null) {
                eq.setQuestionOrder(order);
            } else {
                existing.add(new ExamQuestion(exam, q, order));
            }
        }
    }

    private List<Question> processInlineQuestions(ExamDto dto, Subject subject) {
        List<Question> processed = new ArrayList<>();
        if (dto.getQuestions() != null) {
            for (ExamDto.InlineQuestionDto qDto : dto.getQuestions()) {
                if (qDto.getId() != null) {
                    Question existingQ = questionRepository.findById(qDto.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Question", qDto.getId()));
                    existingQ.setQuestionText(qDto.getQuestionText());
                    existingQ.setOptionA(qDto.getOptionA());
                    existingQ.setOptionB(qDto.getOptionB());
                    existingQ.setOptionC(qDto.getOptionC());
                    existingQ.setOptionD(qDto.getOptionD());
                    existingQ.setCorrectAnswer(qDto.getCorrectAnswer());
                    existingQ.setPositiveMarks(qDto.getPositiveMarks());
                    processed.add(questionRepository.save(existingQ));
                } else {
                    Question newQ = new Question(
                            qDto.getQuestionText(),
                            qDto.getOptionA(),
                            qDto.getOptionB(),
                            qDto.getOptionC(),
                            qDto.getOptionD(),
                            qDto.getCorrectAnswer(),
                            qDto.getPositiveMarks(),
                            subject
                    );
                    processed.add(questionRepository.save(newQ));
                }
            }
        }
        return processed;
    }

    /** Validates cross-field rules not expressible as Bean Validation. */
    private void validateDto(ExamDto dto) {
        if (dto.getAvailabilityMode() == ExamAvailabilityMode.SCHEDULED_PERIOD) {
            if (dto.getValidFrom() == null) {
                throw new IllegalArgumentException("Valid-From date is required for Scheduled Period exams.");
            }
            if (dto.getValidUntil() == null) {
                throw new IllegalArgumentException("Valid-Until date is required for Scheduled Period exams.");
            }
            if (!dto.getValidUntil().isAfter(dto.getValidFrom())) {
                throw new IllegalArgumentException("Valid-Until must be after Valid-From.");
            }
        }
    }

    private void applyAvailabilityFields(Exam exam, ExamDto dto) {
        ExamAvailabilityMode mode = dto.getAvailabilityMode() != null
                ? dto.getAvailabilityMode()
                : ExamAvailabilityMode.NO_EXPIRY;
        exam.setAvailabilityMode(mode);
        exam.setValidFrom(dto.getValidFrom());
        // Clear validUntil for NO_EXPIRY exams to keep the data clean
        exam.setValidUntil(mode == ExamAvailabilityMode.SCHEDULED_PERIOD ? dto.getValidUntil() : null);
    }

    private List<Question> resolveQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return new ArrayList<>();
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new ResourceNotFoundException("One or more question IDs are invalid");
        }
        return questions;
    }



    // -------------------------------------------------------------------------
    // Finders
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Exam findById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
    }

    @Transactional(readOnly = true)
    public ExamDto getExamDtoById(Long id) {
        return toDto(findById(id));
    }

    @Transactional(readOnly = true)
    public List<Exam> findAll() {
        return examRepository.findAll();
    }

    /** @deprecated Use {@link #findAll()} + computeStatus for filtering. */
    @Transactional(readOnly = true)
    public List<Exam> findActiveExams() {
        return examRepository.findByActiveTrue();
    }

    /**
     * Returns all published, non-closed exams that are currently AVAILABLE.
     * Used by the student dashboard.
     */
    @Transactional(readOnly = true)
    public List<Exam> findCurrentlyAvailableExams() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.findByActiveTrueAndManuallyClosedFalse()
                .stream()
                .filter(e -> computeStatus(e, now) == ExamStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    /**
     * Builds the full list of {@link StudentExamView} objects for the dashboard.
     * Includes ALL published exams (upcoming, available, expired) for history.
     */
    @Transactional(readOnly = true)
    public List<StudentExamView> buildStudentExamViews(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> publishedExams = examRepository.findByActiveTrue();

        // Map examId -> attempt for this student (completed or in-progress)
        Map<Long, ExamAttempt> attemptMap = attemptRepository.findByStudentId(studentId)
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getExam().getId(),
                        Function.identity(),
                        // Keep the completed attempt if there are duplicates
                        (a, b) -> a.isCompleted() ? a : b));

        List<StudentExamView> views = new ArrayList<>();
        for (Exam exam : publishedExams) {
            ExamStatus status = computeStatus(exam, now);
            ExamAttempt attempt = attemptMap.get(exam.getId());
            views.add(new StudentExamView(exam, status, attempt));
        }
        return views;
    }

    // -------------------------------------------------------------------------
    // Admin actions
    // -------------------------------------------------------------------------

    public void toggleExamStatus(Long id) {
        Exam exam = findById(id);
        exam.setActive(!exam.isActive());
        examRepository.save(exam);
    }

    public void closeExam(Long id) {
        Exam exam = findById(id);
        exam.setManuallyClosed(true);
        examRepository.save(exam);
    }

    public void reopenExam(Long id) {
        Exam exam = findById(id);
        exam.setManuallyClosed(false);
        examRepository.save(exam);
    }

    public void deleteExam(Long id) {
        examRepository.delete(findById(id));
    }

    // -------------------------------------------------------------------------
    // Queries ordered for admin dashboard
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ExamQuestion> getExamQuestionsOrdered(Long examId) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrder(examId);
    }

    // -------------------------------------------------------------------------
    // Dashboard counts
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public long countAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.countScheduledAvailable(now) + examRepository.countNoExpiryAvailable(now);
    }

    @Transactional(readOnly = true)
    public long countUpcoming() {
        return examRepository.countUpcoming(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countExpired() {
        return examRepository.countExpired(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countClosedOrUnpublished() {
        return examRepository.countByManuallyClosedTrue()
                + examRepository.count() - examRepository.countByActiveTrue();
    }

    // -------------------------------------------------------------------------
    // DTO conversion
    // -------------------------------------------------------------------------

    public ExamDto toDto(Exam exam) {
        ExamDto dto = new ExamDto();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setSubjectId(exam.getSubject().getId());
        dto.setDurationMinutes(exam.getDurationMinutes());
        dto.setPassingPercentage(exam.getPassingPercentage());
        dto.setActive(exam.isActive());
        dto.setAvailabilityMode(exam.getAvailabilityMode());
        dto.setValidFrom(exam.getValidFrom());
        dto.setValidUntil(exam.getValidUntil());
        
        List<ExamDto.InlineQuestionDto> inlineQs = exam.getExamQuestions().stream()
                .sorted(java.util.Comparator.comparingInt(ExamQuestion::getQuestionOrder))
                .map(eq -> {
                    Question q = eq.getQuestion();
                    ExamDto.InlineQuestionDto qDto = new ExamDto.InlineQuestionDto();
                    qDto.setId(q.getId());
                    qDto.setQuestionText(q.getQuestionText());
                    qDto.setOptionA(q.getOptionA());
                    qDto.setOptionB(q.getOptionB());
                    qDto.setOptionC(q.getOptionC());
                    qDto.setOptionD(q.getOptionD());
                    qDto.setCorrectAnswer(q.getCorrectAnswer());
                    qDto.setPositiveMarks(q.getPositiveMarks());
                    return qDto;
                })
                .collect(Collectors.toList());
        
        dto.setQuestions(inlineQs);
        return dto;
    }
}
