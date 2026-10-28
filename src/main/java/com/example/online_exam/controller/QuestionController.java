package com.example.online_exam.controller;

import com.example.online_exam.dto.QuestionDto;
import com.example.online_exam.service.QuestionService;
import com.example.online_exam.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final SubjectService subjectService;

    public QuestionController(QuestionService questionService, SubjectService subjectService) {
        this.questionService = questionService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("questions", questionService.findAll());
        return "admin/questions";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("questionDto", new QuestionDto());
        model.addAttribute("subjects", subjectService.findAll());
        return "admin/question-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("questionDto") QuestionDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("subjects", subjectService.findAll());
            return "admin/question-form";
        }
        try {
            questionService.createQuestion(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Question created successfully");
            return "redirect:/admin/questions";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("subjects", subjectService.findAll());
            return "admin/question-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("questionDto", questionService.toDto(questionService.findById(id)));
        model.addAttribute("subjects", subjectService.findAll());
        return "admin/question-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("questionDto") QuestionDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("subjects", subjectService.findAll());
            return "admin/question-form";
        }
        try {
            questionService.updateQuestion(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Question updated successfully");
            return "redirect:/admin/questions";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("subjects", subjectService.findAll());
            return "admin/question-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.deleteQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/questions";
    }
}
