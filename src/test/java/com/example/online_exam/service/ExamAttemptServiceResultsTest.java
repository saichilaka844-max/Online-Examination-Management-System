package com.example.online_exam.service;

import com.example.online_exam.dto.StudentResultDto;
import com.example.online_exam.entity.*;
import com.example.online_exam.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamAttemptServiceResultsTest {

    @Mock
    private ExamAttemptRepository attemptRepository;
    @Mock
    private ExamQuestionRepository examQuestionRepository;
    @Mock
    private StudentAnswerRepository studentAnswerRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private UserRepository userRepository;

    private ExamAttemptService attemptService;

    @BeforeEach
    void setUp() {
        attemptService = new ExamAttemptService(
                attemptRepository,
                examQuestionRepository,
                studentAnswerRepository,
                examRepository,
                userRepository
        );
    }

    private User createStudent(Long id, String username, String studentId, String fullName, String email) {
        User user = new User(username, "password", Role.STUDENT);
        user.setId(id);
        StudentProfile profile = new StudentProfile(studentId, fullName, "Computer Science", email, user);
        profile.setId(id * 10);
        user.setStudentProfile(profile);
        return user;
    }

    private Exam createExam(Long id, String title, Subject subject, double totalMarks, double passingPercentage) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setTitle(title);
        exam.setSubject(subject);
        exam.setTotalMarks(totalMarks);
        exam.setPassingPercentage(passingPercentage);
        return exam;
    }

    @Test
    @DisplayName("mapToResultDto should map student full name, ID, email, marks, percentage, and question metrics correctly")
    void testMapToResultDto() {
        User student = createStudent(1L, "upendra_user", "STU-001", "Upendra Kumar", "upendra@test.com");
        Subject subject = new Subject("Java", "Core Java Programming");
        subject.setId(101L);
        Exam exam = createExam(10L, "Java Certification Exam", subject, 20.0, 50.0);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(1001L);
        attempt.setStudent(student);
        attempt.setExam(exam);
        attempt.setSubmittedAt(LocalDateTime.of(2026, 10, 14, 15, 30));
        attempt.setScoreObtained(15.0);
        attempt.setCompleted(true);
        attempt.setPassed(true);

        Question q1 = new Question();
        q1.setId(1L);
        q1.setQuestionText("What is JVM?");
        q1.setPositiveMarks(5.0);
        q1.setCorrectAnswer("A");

        Question q2 = new Question();
        q2.setId(2L);
        q2.setQuestionText("Is Java pass-by-value?");
        q2.setPositiveMarks(5.0);
        q2.setCorrectAnswer("B");

        Question q3 = new Question();
        q3.setId(3L);
        q3.setQuestionText("What is bytecode?");
        q3.setPositiveMarks(5.0);
        q3.setCorrectAnswer("C");

        Question q4 = new Question();
        q4.setId(4L);
        q4.setQuestionText("What is GC?");
        q4.setPositiveMarks(5.0);
        q4.setCorrectAnswer("D");

        StudentAnswer a1 = new StudentAnswer(attempt, q1, "A");
        a1.setCorrect(true);
        a1.setMarksAwarded(5.0);

        StudentAnswer a2 = new StudentAnswer(attempt, q2, "B");
        a2.setCorrect(true);
        a2.setMarksAwarded(5.0);

        StudentAnswer a3 = new StudentAnswer(attempt, q3, "A"); // Wrong answer
        a3.setCorrect(false);
        a3.setMarksAwarded(0.0);

        StudentAnswer a4 = new StudentAnswer(attempt, q4, null); // Unattempted
        a4.setCorrect(false);
        a4.setMarksAwarded(0.0);

        List<StudentAnswer> answers = List.of(a1, a2, a3, a4);

        StudentResultDto dto = attemptService.mapToResultDto(attempt, answers);

        assertNotNull(dto);
        assertEquals(1001L, dto.getResultId());
        assertEquals("Upendra Kumar", dto.getStudentName(), "Must display student full name, not ID");
        assertEquals("STU-001", dto.getStudentId());
        assertEquals("upendra@test.com", dto.getStudentEmail());
        assertEquals("Java Certification Exam", dto.getExamTitle());
        assertEquals("Java", dto.getSubjectName());
        assertEquals(20.0, dto.getTotalMarks());
        assertEquals(15.0, dto.getMarksObtained());
        assertEquals(75.0, dto.getPercentage());
        assertTrue(dto.isPassed());

        assertEquals(4, dto.getTotalQuestions());
        assertEquals(3, dto.getAttemptedQuestions());
        assertEquals(2, dto.getCorrectAnswers());
        assertEquals(1, dto.getWrongAnswers());
    }

    @Test
    @DisplayName("findFilteredResults should filter by student name, exam, status, and sort correctly")
    void testFindFilteredResults() {
        User student1 = createStudent(1L, "user1", "STU1", "Alice Smith", "alice@test.com");
        User student2 = createStudent(2L, "user2", "STU2", "Bob Jones", "bob@test.com");

        Subject subject = new Subject("DSA", "Data Structures");
        subject.setId(201L);
        Exam exam1 = createExam(21L, "DSA Midterm", subject, 100.0, 60.0);
        Exam exam2 = createExam(22L, "DSA Final", subject, 100.0, 60.0);

        ExamAttempt att1 = new ExamAttempt();
        att1.setId(1L);
        att1.setStudent(student1);
        att1.setExam(exam1);
        att1.setScoreObtained(85.0);
        att1.setPassed(true);
        att1.setCompleted(true);
        att1.setSubmittedAt(LocalDateTime.of(2026, 10, 10, 10, 0));

        ExamAttempt att2 = new ExamAttempt();
        att2.setId(2L);
        att2.setStudent(student2);
        att2.setExam(exam1);
        att2.setScoreObtained(45.0);
        att2.setPassed(false);
        att2.setCompleted(true);
        att2.setSubmittedAt(LocalDateTime.of(2026, 10, 11, 10, 0));

        ExamAttempt att3 = new ExamAttempt();
        att3.setId(3L);
        att3.setStudent(student1);
        att3.setExam(exam2);
        att3.setScoreObtained(95.0);
        att3.setPassed(true);
        att3.setCompleted(true);
        att3.setSubmittedAt(LocalDateTime.of(2026, 10, 12, 10, 0));

        when(attemptRepository.findAllCompleted()).thenReturn(List.of(att1, att2, att3));
        when(studentAnswerRepository.findByAttemptIdIn(anyList())).thenReturn(Collections.emptyList());

        // 1. Filter by student name "alice"
        List<StudentResultDto> aliceResults = attemptService.findFilteredResults(
                "alice", null, null, null, null, "ALL", "date_desc");
        assertEquals(2, aliceResults.size());
        assertTrue(aliceResults.stream().allMatch(r -> r.getStudentName().equals("Alice Smith")));

        // 2. Filter by status "FAIL"
        List<StudentResultDto> failedResults = attemptService.findFilteredResults(
                null, null, null, null, null, "FAIL", "date_desc");
        assertEquals(1, failedResults.size());
        assertEquals("Bob Jones", failedResults.get(0).getStudentName());

        // 3. Filter by exam2
        List<StudentResultDto> exam2Results = attemptService.findFilteredResults(
                null, null, null, 22L, null, "ALL", "date_desc");
        assertEquals(1, exam2Results.size());
        assertEquals("DSA Final", exam2Results.get(0).getExamTitle());

        // 4. Sort by score descending
        List<StudentResultDto> sortedByScore = attemptService.findFilteredResults(
                null, null, null, null, null, "ALL", "score_desc");
        assertEquals(3, sortedByScore.size());
        assertEquals(95.0, sortedByScore.get(0).getMarksObtained());
        assertEquals(85.0, sortedByScore.get(1).getMarksObtained());
        assertEquals(45.0, sortedByScore.get(2).getMarksObtained());
    }
}
