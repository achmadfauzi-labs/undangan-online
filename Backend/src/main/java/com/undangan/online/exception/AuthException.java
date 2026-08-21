package com.undangan.online.exception;

public class AuthException extends RuntimeException {
    private final String errorCode;
    private final int status;

    public AuthException(String errorCode, String message, int status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatus() {
        return status;
    }
}
