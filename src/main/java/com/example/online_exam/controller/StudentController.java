package com.example.online_exam.controller;

import com.example.online_exam.dto.ExamSubmissionDto;
import com.example.online_exam.dto.StudentExamView;
import com.example.online_exam.entity.*;
import com.example.online_exam.service.ExamAttemptService;
import com.example.online_exam.service.ExamService;
import com.example.online_exam.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ExamService examService;
    private final ExamAttemptService attemptService;
    private final UserService userService;

    public StudentController(ExamService examService,
                             ExamAttemptService attemptService,
                             UserService userService) {
        this.examService = examService;
        this.attemptService = attemptService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.findByUsername(userDetails.getUsername());
        StudentProfile profile = student.getStudentProfile();

        // All published exam views with status + attempt info
        List<StudentExamView> allViews = examService.buildStudentExamViews(student.getId());

        long availableCount = allViews.stream()
                .filter(v -> v.getStatus() == ExamStatus.AVAILABLE && !v.isAttempted())
                .count();
        long upcomingCount = allViews.stream()
                .filter(v -> v.getStatus() == ExamStatus.UPCOMING)
                .count();
        long expiredCount = allViews.stream()
                .filter(v -> v.getStatus() == ExamStatus.EXPIRED)
                .count();
        long submittedCount = allViews.stream()
                .filter(StudentExamView::isAttempted)
                .count();

        model.addAttribute("student", student);
        model.addAttribute("profile", profile);
        model.addAttribute("allViews", allViews);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("submittedCount", submittedCount);
        return "student/dashboard";
    }

    @GetMapping("/exams/{examId}/start")
    public String startExam(@PathVariable Long examId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findByUsername(userDetails.getUsername());
            ExamAttempt attempt = attemptService.startAttempt(examId, student.getId());
            
            if (attempt.isCompleted()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your previous attempt has expired and has been automatically submitted.");
                return "redirect:/student/dashboard";
            }
            
            List<ExamQuestion> examQuestions = examService.getExamQuestionsOrdered(examId);

            // Remaining seconds from the server-stored deadline (or legacy fallback)
            LocalDateTime deadline = ExamAttemptService.effectiveDeadline(attempt);
            long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), deadline).getSeconds();
            if (remainingSeconds < 0) remainingSeconds = 0;

            model.addAttribute("attempt", attempt);
            model.addAttribute("exam", attempt.getExam());
            model.addAttribute("examQuestions", examQuestions);
            model.addAttribute("remainingSeconds", remainingSeconds);
            ExamSubmissionDto submissionDto = new ExamSubmissionDto();
            submissionDto.setAttemptId(attempt.getId());
            model.addAttribute("submission", submissionDto);
            return "student/take-exam";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/dashboard";
        }
    }

    @PostMapping("/exams/submit")
    public String submitExam(@ModelAttribute ExamSubmissionDto submission,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findByUsername(userDetails.getUsername());
            ExamAttempt attempt = attemptService.submitAttempt(submission, student.getId());
            return "redirect:/student/results/" + attempt.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/dashboard";
        }
    }

    @GetMapping("/results/{attemptId}")
    public String viewResult(@PathVariable Long attemptId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            ExamAttempt attempt = attemptService.findById(attemptId);
            User student = userService.findByUsername(userDetails.getUsername());

            if (!attempt.getStudent().getId().equals(student.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied");
                return "redirect:/student/dashboard";
            }

            model.addAttribute("attempt", attempt);
            model.addAttribute("answers", attemptService.getAnswersForAttempt(attemptId));
            return "student/result";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/dashboard";
        }
    }
}
