package com.mobilePrepaid.system.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.mobilePrepaid.system.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("sreeadvaitha2206@gmail.com");
        try {
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void sendPaymentConfirmationEmail(String to, String mobileNumber, double amount, int validityDays, 
            String data, String calls, String sms, String subscriberName, String transactionId) {
        String subject = "Payment Confirmation";
        String body = String.format(
            "Dear %s,\n\nYour payment for mobile number %s has been successfully processed.\n\n" +
            "Payment Details:\n" +
            "Transaction ID: %s\n" +
            "Amount: ₹%.2f\nValidity: %d days\nData: %s GB/Day\nCalls: %s\nSMS: %s\n\n" +
            "Thank you for choosing Mobi-Comm!",
            subscriberName, mobileNumber, transactionId, amount, validityDays, data, calls, sms
        );
        sendEmail(to, subject, body);
    }
}