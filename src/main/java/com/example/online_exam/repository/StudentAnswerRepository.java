package com.example.online_exam.repository;

import com.example.online_exam.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"question"})
    List<StudentAnswer> findByAttemptId(Long attemptId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"question"})
    List<StudentAnswer> findByAttemptIdIn(List<Long> attemptIds);

    Optional<StudentAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
