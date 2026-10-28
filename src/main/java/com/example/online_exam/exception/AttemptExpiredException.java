package com.example.online_exam.exception;

public class AttemptExpiredException extends RuntimeException {
    public AttemptExpiredException(String message) {
        super(message);
    }
}
