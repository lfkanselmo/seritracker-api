package com.seritracker.domain.exception;

public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("Invalid or expired reset token");
    }
}
