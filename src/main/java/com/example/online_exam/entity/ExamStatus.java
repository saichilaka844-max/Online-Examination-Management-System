package com.example.online_exam.entity;

/**
 * Computed availability status of an exam at a given instant.
 *
 * <ul>
 *   <li>{@code UNPUBLISHED}  – admin has not yet set active=true.</li>
 *   <li>{@code UPCOMING}     – published but validFrom is in the future.</li>
 *   <li>{@code AVAILABLE}    – published, open, within the scheduled window.</li>
 *   <li>{@code EXPIRED}      – published but validUntil is in the past (SCHEDULED_PERIOD).</li>
 *   <li>{@code CLOSED}       – admin manually closed the exam.</li>
 * </ul>
 */
public enum ExamStatus {
    UNPUBLISHED,
    UPCOMING,
    AVAILABLE,
    EXPIRED,
    CLOSED
}
