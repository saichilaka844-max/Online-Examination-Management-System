package com.example.online_exam.repository;

import com.example.online_exam.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    List<ExamAttempt> findByStudentId(Long studentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"exam", "exam.subject", "student", "student.studentProfile"})
    List<ExamAttempt> findByExamId(Long examId);

    Optional<ExamAttempt> findByExamIdAndStudentIdAndCompletedFalse(Long examId, Long studentId);

    boolean existsByExamIdAndStudentIdAndCompletedTrue(Long examId, Long studentId);

    List<ExamAttempt> findByStudentIdAndCompletedTrue(Long studentId);

    /** All completed or in-progress attempts for a student. */
    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    /** Count of all completed attempts (across all students). */
    long countByCompletedTrue();

    /** All completed attempts with associations eagerly fetched, newest first. */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"exam", "exam.subject", "student", "student.studentProfile"})
    @Query("SELECT a FROM ExamAttempt a WHERE a.completed = true ORDER BY a.submittedAt DESC")
    List<ExamAttempt> findAllCompleted();

    /** Count of completed attempts per exam (used in admin dashboard). */
    @Query("SELECT COUNT(a) FROM ExamAttempt a WHERE a.exam.id = :examId AND a.completed = true")
    long countCompletedByExamId(@Param("examId") Long examId);

    /** Find attempt by id – included for clarity even though inherited from JpaRepository. */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"exam", "exam.subject", "student", "student.studentProfile"})
    Optional<ExamAttempt> findById(Long id);

    /** All completed attempts for a student, newest first. */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"exam", "exam.subject"})
    List<ExamAttempt> findByStudentIdAndCompletedTrueOrderBySubmittedAtDesc(Long studentId);
}
