package com.mobilePrepaid.system.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendPaymentConfirmationEmail(String to, String mobileNumber, double amount, int validityDays, 
            String data, String calls, String sms, String subscriberName, String transactionId);
}