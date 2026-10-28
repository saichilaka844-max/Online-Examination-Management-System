package com.example.online_exam.service;

import com.example.online_exam.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ExamService#computeStatus(Exam, LocalDateTime)}.
 * No Spring context is loaded – pure logic tests.
 */
class ExamServiceStatusTest {

    private Exam exam;
    private final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 12, 0, 0);

    @BeforeEach
    void setUp() {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");

        exam = new Exam();
        exam.setId(1L);
        exam.setTitle("Test Exam");
        exam.setSubject(subject);
        exam.setDurationMinutes(60);
        exam.setTotalMarks(100);
        exam.setPassingPercentage(40);
        exam.setActive(false);
        exam.setManuallyClosed(false);
        exam.setAvailabilityMode(ExamAvailabilityMode.NO_EXPIRY);
    }

    // ---- UNPUBLISHED ----

    @Test
    void testUnpublished_whenActiveFalse() {
        exam.setActive(false);
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.UNPUBLISHED);
    }

    // ---- CLOSED ----

    @Test
    void testClosed_whenManuallyClosedTrue() {
        exam.setActive(true);
        exam.setManuallyClosed(true);
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.CLOSED);
    }

    @Test
    void testClosed_overridesValidWindow() {
        exam.setActive(true);
        exam.setManuallyClosed(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.SCHEDULED_PERIOD);
        exam.setValidFrom(NOW.minusHours(1));
        exam.setValidUntil(NOW.plusHours(1));
        // Even though we're within the scheduled window, closed takes priority
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.CLOSED);
    }

    // ---- UPCOMING ----

    @Test
    void testUpcoming_whenValidFromInFuture_noExpiry() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.NO_EXPIRY);
        exam.setValidFrom(NOW.plusHours(2));
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.UPCOMING);
    }

    @Test
    void testUpcoming_whenValidFromInFuture_scheduled() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.SCHEDULED_PERIOD);
        exam.setValidFrom(NOW.plusDays(1));
        exam.setValidUntil(NOW.plusDays(2));
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.UPCOMING);
    }

    // ---- AVAILABLE ----

    @Test
    void testAvailable_noExpiryWithoutValidFrom() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.NO_EXPIRY);
        exam.setValidFrom(null);
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.AVAILABLE);
    }

    @Test
    void testAvailable_noExpiryWithPastValidFrom() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.NO_EXPIRY);
        exam.setValidFrom(NOW.minusHours(5));
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.AVAILABLE);
    }

    @Test
    void testAvailable_scheduledWithinWindow() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.SCHEDULED_PERIOD);
        exam.setValidFrom(NOW.minusHours(1));
        exam.setValidUntil(NOW.plusHours(1));
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.AVAILABLE);
    }

    // ---- EXPIRED ----

    @Test
    void testExpired_scheduledPastValidUntil() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.SCHEDULED_PERIOD);
        exam.setValidFrom(NOW.minusDays(2));
        exam.setValidUntil(NOW.minusHours(1));
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.EXPIRED);
    }

    @Test
    void testExpired_exactlyAtValidUntil() {
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.SCHEDULED_PERIOD);
        exam.setValidFrom(NOW.minusDays(1));
        exam.setValidUntil(NOW); // exact boundary
        // validUntil is NOT after now → expired
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.EXPIRED);
    }

    @Test
    void testNoExpiry_doesNotExpireWhenValidUntilSet() {
        // NO_EXPIRY mode should IGNORE validUntil
        exam.setActive(true);
        exam.setAvailabilityMode(ExamAvailabilityMode.NO_EXPIRY);
        exam.setValidFrom(NOW.minusDays(1));
        exam.setValidUntil(NOW.minusHours(1)); // set but should be ignored
        assertThat(ExamService.computeStatus(exam, NOW)).isEqualTo(ExamStatus.AVAILABLE);
    }
}
