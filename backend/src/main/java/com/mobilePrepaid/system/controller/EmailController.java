package com.mobilePrepaid.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mobilePrepaid.system.dto.EmailRequest;
import com.mobilePrepaid.system.dto.EmailResponse;
import com.mobilePrepaid.system.service.EmailService;

import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class EmailController {
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private EmailService emailService;
    @PostMapping("/api/send-email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        try {
            String subject = "Payment Confirmation";
            String body = String.format(
                "Dear %s,\n\nYour payment has been successfully processed.\n\nPayment Details:\n" +
                "Transaction ID: %s\nPrice: %s\nValidity: %s days\nData: %s\nCalls: %s\nSMS: %s\n\n" +
                "Thank you for choosing Mobi-Comm!",
                request.getSubscriberName(), // Username as greeting
                request.getTransaction_id(), // Transaction ID
                request.getPrice(),
                request.getValidity(),
                request.getData(),
                request.getCalls(),
                request.getSms()
            );
            emailService.sendEmail(request.getEmail(), subject, body);
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
}