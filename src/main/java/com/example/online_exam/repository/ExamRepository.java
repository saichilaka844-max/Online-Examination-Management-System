package com.example.online_exam.repository;

import com.example.online_exam.entity.Exam;
import com.example.online_exam.entity.ExamAvailabilityMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByActiveTrue();

    List<Exam> findBySubjectId(Long subjectId);

    /** All published, non-closed exams (regardless of schedule). */
    List<Exam> findByActiveTrueAndManuallyClosedFalse();

    /**
     * Count exams with a specific availability mode.
     */
    long countByAvailabilityMode(ExamAvailabilityMode mode);

    /**
     * Count published exams.
     */
    long countByActiveTrue();

    /**
     * Count manually closed exams.
     */
    long countByManuallyClosedTrue();

    /**
     * Count published + not manually closed + SCHEDULED_PERIOD exams
     * that are currently within the valid window.
     */
    @Query("""
            SELECT COUNT(e) FROM Exam e
            WHERE e.active = true
              AND e.manuallyClosed = false
              AND e.availabilityMode = 'SCHEDULED_PERIOD'
              AND e.validFrom <= :now
              AND e.validUntil > :now
            """)
    long countScheduledAvailable(@Param("now") LocalDateTime now);

    /**
     * Count published + not manually closed + NO_EXPIRY exams
     * that have already opened (validFrom <= now, or validFrom is null).
     */
    @Query("""
            SELECT COUNT(e) FROM Exam e
            WHERE e.active = true
              AND e.manuallyClosed = false
              AND e.availabilityMode = 'NO_EXPIRY'
              AND (e.validFrom IS NULL OR e.validFrom <= :now)
            """)
    long countNoExpiryAvailable(@Param("now") LocalDateTime now);

    /**
     * Count published, non-closed exams whose window hasn't opened yet.
     */
    @Query("""
            SELECT COUNT(e) FROM Exam e
            WHERE e.active = true
              AND e.manuallyClosed = false
              AND e.validFrom IS NOT NULL
              AND e.validFrom > :now
            """)
    long countUpcoming(@Param("now") LocalDateTime now);

    /**
     * Count published SCHEDULED_PERIOD exams whose validUntil has passed.
     */
    @Query("""
            SELECT COUNT(e) FROM Exam e
            WHERE e.active = true
              AND e.manuallyClosed = false
              AND e.availabilityMode = 'SCHEDULED_PERIOD'
              AND e.validUntil <= :now
            """)
    long countExpired(@Param("now") LocalDateTime now);
}
