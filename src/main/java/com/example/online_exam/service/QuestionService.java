package com.example.online_exam.service;

import com.example.online_exam.dto.QuestionDto;
import com.example.online_exam.entity.Question;
import com.example.online_exam.entity.Subject;
import com.example.online_exam.exception.ResourceNotFoundException;
import com.example.online_exam.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SubjectService subjectService;

    public QuestionService(QuestionRepository questionRepository, SubjectService subjectService) {
        this.questionRepository = questionRepository;
        this.subjectService = subjectService;
    }

    public Question createQuestion(QuestionDto dto) {
        Subject subject = subjectService.findById(dto.getSubjectId());
        Question question = new Question(
                dto.getQuestionText(),
                dto.getOptionA(),
                dto.getOptionB(),
                dto.getOptionC(),
                dto.getOptionD(),
                dto.getCorrectAnswer(),
                dto.getPositiveMarks(),
                subject
        );
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, QuestionDto dto) {
        Question question = findById(id);
        Subject subject = subjectService.findById(dto.getSubjectId());

        question.setQuestionText(dto.getQuestionText());
        question.setOptionA(dto.getOptionA());
        question.setOptionB(dto.getOptionB());
        question.setOptionC(dto.getOptionC());
        question.setOptionD(dto.getOptionD());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setPositiveMarks(dto.getPositiveMarks());
        question.setSubject(subject);

        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public Question findById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    @Transactional(readOnly = true)
    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Question> findBySubjectId(Long subjectId) {
        return questionRepository.findBySubjectId(subjectId);
    }

    public void deleteQuestion(Long id) {
        Question question = findById(id);
        questionRepository.delete(question);
    }

    /** Converts a Question entity to its DTO representation. */
    public QuestionDto toDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setOptionA(question.getOptionA());
        dto.setOptionB(question.getOptionB());
        dto.setOptionC(question.getOptionC());
        dto.setOptionD(question.getOptionD());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setPositiveMarks(question.getPositiveMarks());
        dto.setSubjectId(question.getSubject().getId());
        return dto;
    }
}
