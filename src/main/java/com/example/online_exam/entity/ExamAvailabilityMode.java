package com.example.online_exam.entity;

/**
 * Controls how an exam's availability window is determined.
 *
 * <ul>
 *   <li>{@code SCHEDULED_PERIOD} – the exam is available only between
 *       {@code validFrom} and {@code validUntil}.</li>
 *   <li>{@code NO_EXPIRY} – the exam opens on {@code validFrom} and remains
 *       available indefinitely until the admin manually closes or unpublishes it.</li>
 * </ul>
 *
 * Existing records that were created before this column existed default to
 * {@code NO_EXPIRY} so their current behaviour is preserved.
 */
public enum ExamAvailabilityMode {
    SCHEDULED_PERIOD,
    NO_EXPIRY
}
