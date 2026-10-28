package com.example.online_exam.config;

import com.example.online_exam.entity.*;
import com.example.online_exam.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TestDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataInitializer(UserRepository userRepository,
                               SubjectRepository subjectRepository,
                               QuestionRepository questionRepository,
                               ExamRepository examRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Create student
        if (!userRepository.existsByUsername("student")) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("student"));
            student.setRole(Role.STUDENT);
            userRepository.save(student);
        }

        if (subjectRepository.count() == 0) {
            Subject subject = new Subject();
            subject.setName("Math");
            subject.setDescription("Math Subject");
            subjectRepository.save(subject);

            Question q1 = new Question();
            q1.setQuestionText("1 + 1 = ?");
            q1.setSubject(subject);
            q1.setOptionA("1");
            q1.setOptionB("2");
            q1.setOptionC("3");
            q1.setOptionD("4");
            q1.setCorrectAnswer("B");
            q1.setPositiveMarks(10);
            questionRepository.save(q1);

            Exam exam = new Exam();
            exam.setTitle("Math Exam 1");
            exam.setSubject(subject);
            exam.setDurationMinutes(60);
            exam.setTotalMarks(10);
            exam.setPassingPercentage(50);
            exam.setActive(true);
            exam.setAvailabilityMode(ExamAvailabilityMode.NO_EXPIRY);
            
            ExamQuestion eq = new ExamQuestion();
            eq.setExam(exam);
            eq.setQuestion(q1);
            eq.setQuestionOrder(1);
            exam.setExamQuestions(List.of(eq));
            
            examRepository.save(exam);
        }
    }
}
