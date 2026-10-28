package com.example.online_exam.controller;

import com.example.online_exam.dto.ExamDto;
import com.example.online_exam.entity.ExamAvailabilityMode;
import com.example.online_exam.service.ExamService;
import com.example.online_exam.service.QuestionService;
import com.example.online_exam.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/exams")
public class ExamController {

    private final ExamService examService;
    private final SubjectService subjectService;
    private final QuestionService questionService;

    public ExamController(ExamService examService,
                          SubjectService subjectService,
                          QuestionService questionService) {
        this.examService = examService;
        this.subjectService = subjectService;
        this.questionService = questionService;
    }

    private void populateFormModel(Model model) {
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("questions", questionService.findAll());
        model.addAttribute("availabilityModes", ExamAvailabilityMode.values());
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("examDto", new ExamDto());
        populateFormModel(model);
        return "admin/exam-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("examDto") ExamDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateFormModel(model);
            return "admin/exam-form";
        }
        try {
            examService.createExam(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Exam created successfully");
            return "redirect:/admin/exams";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model);
            return "admin/exam-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("examDto", examService.getExamDtoById(id));
        populateFormModel(model);
        return "admin/exam-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("examDto") ExamDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateFormModel(model);
            return "admin/exam-form";
        }
        try {
            examService.updateExam(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Exam updated successfully");
            return "redirect:/admin/exams";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model);
            return "admin/exam-form";
        }
    }
}
