package com.example.online_exam.controller;

import com.example.online_exam.dto.UserRegistrationDto;
import com.example.online_exam.entity.Exam;
import com.example.online_exam.entity.ExamStatus;
import com.example.online_exam.entity.Role;
import com.example.online_exam.entity.Subject;
import com.example.online_exam.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final SubjectService subjectService;
    private final ExamService examService;
    private final ExamAttemptService attemptService;

    public AdminController(UserService userService,
                           SubjectService subjectService,
                           ExamService examService,
                           ExamAttemptService attemptService) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.examService = examService;
        this.attemptService = attemptService;
    }

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalStudents   = userService.findAllStudents().size();
        long totalExams      = examService.findAll().size();
        long totalSubjects   = subjectService.findAll().size();
        long totalAttempts   = attemptService.countAllCompleted();
        long availableExams  = examService.countAvailable();
        long upcomingExams   = examService.countUpcoming();
        long expiredExams    = examService.countExpired();
        long closedExams     = examService.countClosedOrUnpublished();

        model.addAttribute("totalStudents",  totalStudents);
        model.addAttribute("totalExams",     totalExams);
        model.addAttribute("totalSubjects",  totalSubjects);
        model.addAttribute("totalAttempts",  totalAttempts);
        model.addAttribute("availableExams", availableExams);
        model.addAttribute("upcomingExams",  upcomingExams);
        model.addAttribute("expiredExams",   expiredExams);
        model.addAttribute("closedExams",    closedExams);

        // Build exam summary list for the admin table
        LocalDateTime now = LocalDateTime.now();
        List<Exam> exams = examService.findAll();
        // Attach computed status so Thymeleaf can display it
        model.addAttribute("exams", exams);
        // Helper: the status must be computed in the template via ExamService.computeStatus
        // We pass the list and let the template call the service via a map provided here.
        // Simpler: compute a parallel list of statuses.
        List<ExamStatus> statuses = exams.stream()
                .map(e -> ExamService.computeStatus(e, now))
                .toList();
        model.addAttribute("examStatuses", statuses);
        // Attempt counts per exam
        List<Long> attemptCounts = exams.stream()
                .map(e -> attemptService.findByExam(e.getId()).size())
                .map(Long::valueOf)
                .toList();
        model.addAttribute("examAttemptCounts", attemptCounts);

        return "admin/dashboard";
    }

    // ---- User Management ----

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("registrationDto", new UserRegistrationDto());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@ModelAttribute UserRegistrationDto dto,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(dto);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        model.addAttribute("userObj",
                userService.findByUsername(
                        userService.findAll().stream()
                                .filter(u -> u.getId().equals(id))
                                .findFirst()
                                .orElseThrow()
                                .getUsername()));
        return "admin/user-details";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ---- Subject Management ----

    @GetMapping("/subjects")
    public String listSubjects(Model model) {
        model.addAttribute("subjects", subjectService.findAll());
        return "admin/subjects";
    }

    @PostMapping("/subjects/create")
    public String createSubject(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            subjectService.createSubject(name, description);
            redirectAttributes.addFlashAttribute("successMessage", "Subject created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/delete")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            subjectService.deleteSubject(id);
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @GetMapping("/subjects/{id}/edit")
    public String editSubjectForm(@PathVariable Long id, Model model) {
        model.addAttribute("subject", subjectService.findById(id));
        return "admin/subject-form";
    }

    @PostMapping("/subjects/{id}/edit")
    public String updateSubject(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            subjectService.updateSubject(id, name, description);
            redirectAttributes.addFlashAttribute("successMessage", "Subject updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    // ---- Exam Management ----

    @GetMapping("/exams")
    public String listExams(Model model) {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> exams = examService.findAll();
        List<ExamStatus> statuses = exams.stream()
                .map(e -> ExamService.computeStatus(e, now))
                .toList();
        List<Long> attemptCounts = exams.stream()
                .map(e -> (long) attemptService.findByExam(e.getId()).size())
                .toList();
        model.addAttribute("exams", exams);
        model.addAttribute("examStatuses", statuses);
        model.addAttribute("examAttemptCounts", attemptCounts);
        return "admin/exams";
    }

    @GetMapping("/exams/{id}/results")
    public String examResults(@PathVariable Long id, Model model) {
        model.addAttribute("exam", examService.findById(id));
        model.addAttribute("attempts", attemptService.findByExam(id));
        return "admin/exam-results";
    }

    @PostMapping("/exams/{id}/toggle")
    public String toggleExamStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.toggleExamStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Exam publication status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/exams";
    }

    @PostMapping("/exams/{id}/close")
    public String closeExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.closeExam(id);
            redirectAttributes.addFlashAttribute("successMessage", "Exam manually closed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/exams";
    }

    @PostMapping("/exams/{id}/reopen")
    public String reopenExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.reopenExam(id);
            redirectAttributes.addFlashAttribute("successMessage", "Exam reopened");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/exams";
    }

    @PostMapping("/exams/{id}/delete")
    public String deleteExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.deleteExam(id);
            redirectAttributes.addFlashAttribute("successMessage", "Exam deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/exams";
    }

    // ---- Student Results Management ----

    @GetMapping("/results")
    public String listResults(@RequestParam(required = false) String studentName,
                              @RequestParam(required = false) String studentId,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) Long examId,
                              @RequestParam(required = false) Long subjectId,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false, defaultValue = "date_desc") String sortBy,
                              Model model) {
        List<com.example.online_exam.dto.StudentResultDto> results = attemptService.findFilteredResults(
                studentName, studentId, email, examId, subjectId, status, sortBy);

        model.addAttribute("results", results);
        model.addAttribute("totalCompleted", attemptService.countAllCompleted());
        model.addAttribute("exams", examService.findAll());
        model.addAttribute("subjects", subjectService.findAll());

        // Pass filter criteria back to preserve form state
        model.addAttribute("filterStudentName", studentName != null ? studentName.trim() : "");
        model.addAttribute("filterStudentId", studentId != null ? studentId.trim() : "");
        model.addAttribute("filterEmail", email != null ? email.trim() : "");
        model.addAttribute("filterExamId", examId);
        model.addAttribute("filterSubjectId", subjectId);
        model.addAttribute("filterStatus", status != null ? status : "ALL");
        model.addAttribute("filterSortBy", sortBy != null ? sortBy : "date_desc");

        return "admin/results";
    }

    // ---- Result Details ----

    @GetMapping({"/results/{id}", "/attempts/{id}"})
    public String viewAttemptDetails(@PathVariable Long id, Model model) {
        com.example.online_exam.entity.ExamAttempt attempt = attemptService.findById(id);
        List<com.example.online_exam.entity.StudentAnswer> answers = attemptService.getAnswersForAttempt(id);
        com.example.online_exam.dto.StudentResultDto result = attemptService.mapToResultDto(attempt, answers);

        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", answers);
        model.addAttribute("result", result);
        return "admin/attempt-details";
    }
}
