package com.mobilePrepaid.system.dto;

public class VerifyNumberResponse {
    private String status;
    private String message;
    private Long subscriberId;
    private String token; // Add token field

    // Constructor for success response
    public VerifyNumberResponse(String status, String message, Long subscriberId, String token) {
        this.status = status;
        this.message = message;
        this.subscriberId = subscriberId;
        this.token = token;
    }

    // Constructor for error response
    public VerifyNumberResponse(String status, String message, Long subscriberId) {
        this.status = status;
        this.message = message;
        this.subscriberId = subscriberId;
    }

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}