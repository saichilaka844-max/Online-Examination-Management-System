package com.example.online_exam.controller;

import com.example.online_exam.dto.StudentResultDto;
import com.example.online_exam.entity.*;
import com.example.online_exam.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminResultsControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SubjectService subjectService;

    @Mock
    private ExamService examService;

    @Mock
    private ExamAttemptService attemptService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(userService, subjectService, examService, attemptService);
    }

    @Test
    @DisplayName("listResults should return admin/results view and populate all filter attributes and model results")
    void testListResults() {
        StudentResultDto dto = new StudentResultDto();
        dto.setResultId(10L);
        dto.setStudentName("Upendra");
        dto.setStudentId("STU-100");
        dto.setStudentEmail("upendra@test.com");
        dto.setExamTitle("Java Basics");
        dto.setSubjectName("Java");
        dto.setSubmittedAt(LocalDateTime.now());
        dto.setTotalMarks(10.0);
        dto.setMarksObtained(8.0);
        dto.setPercentage(80.0);
        dto.setPassed(true);
        dto.setTotalQuestions(5);
        dto.setAttemptedQuestions(5);
        dto.setCorrectAnswers(4);
        dto.setWrongAnswers(1);

        when(attemptService.findFilteredResults(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(dto));
        when(attemptService.countAllCompleted()).thenReturn(1L);
        when(examService.findAll()).thenReturn(Collections.emptyList());
        when(subjectService.findAll()).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = adminController.listResults(
                "Upendra", "STU-100", "upendra@test.com", 1L, 2L, "PASS", "score_desc", model);

        assertEquals("admin/results", view);
        assertTrue(model.containsAttribute("results"));
        assertTrue(model.containsAttribute("totalCompleted"));
        assertTrue(model.containsAttribute("exams"));
        assertTrue(model.containsAttribute("subjects"));
        assertEquals("Upendra", model.getAttribute("filterStudentName"));
        assertEquals("STU-100", model.getAttribute("filterStudentId"));
        assertEquals("upendra@test.com", model.getAttribute("filterEmail"));
        assertEquals(1L, model.getAttribute("filterExamId"));
        assertEquals(2L, model.getAttribute("filterSubjectId"));
        assertEquals("PASS", model.getAttribute("filterStatus"));
        assertEquals("score_desc", model.getAttribute("filterSortBy"));

        @SuppressWarnings("unchecked")
        List<StudentResultDto> results = (List<StudentResultDto>) model.getAttribute("results");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Upendra", results.get(0).getStudentName());
    }

    @Test
    @DisplayName("viewAttemptDetails should return admin/attempt-details view and populate result & answers model")
    void testViewAttemptDetails() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setTitle("Spring Boot Basics");

        User student = new User("alice", "pass", Role.STUDENT);
        student.setId(2L);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(10L);
        attempt.setExam(exam);
        attempt.setStudent(student);

        StudentResultDto dto = new StudentResultDto();
        dto.setResultId(10L);
        dto.setStudentName("Alice");

        when(attemptService.findById(10L)).thenReturn(attempt);
        when(attemptService.getAnswersForAttempt(10L)).thenReturn(Collections.emptyList());
        when(attemptService.mapToResultDto(eq(attempt), anyList())).thenReturn(dto);

        Model model = new ConcurrentModel();
        String view = adminController.viewAttemptDetails(10L, model);

        assertEquals("admin/attempt-details", view);
        assertEquals(attempt, model.getAttribute("attempt"));
        assertEquals(dto, model.getAttribute("result"));
        assertTrue(model.containsAttribute("answers"));
    }
}
