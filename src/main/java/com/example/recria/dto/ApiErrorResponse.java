package com.example.recria.dto;

import java.util.Map;

public class ApiErrorResponse {
    private String errorCode;
    private String message;
    private Map<String, String> details;

    public ApiErrorResponse() {}

    public ApiErrorResponse(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApiErrorResponse(String errorCode, String message, Map<String, String> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }
}
