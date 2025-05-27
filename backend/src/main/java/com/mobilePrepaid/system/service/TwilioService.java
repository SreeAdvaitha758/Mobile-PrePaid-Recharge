package com.mobilePrepaid.system.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class TwilioService {

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    // Simple in-memory store for OTPs (use Redis or DB in production)
    private final Map<String, String> otpStore = new HashMap<>();

    public String sendOTP(String mobileNumber) {
        String formattedNumber = "+91" + mobileNumber; // Assuming Indian numbers
        String otp = generateOTP();

        Message message = Message.creator(
                new PhoneNumber(formattedNumber),
                new PhoneNumber(twilioPhoneNumber),
                "Your Mobi-Comm OTP is: " + otp + ". Valid for 5 minutes."
        ).create();

        otpStore.put(mobileNumber, otp);
        // In production, add expiration logic (e.g., 5 minutes)
        return otp; // For testing; remove in production
    }

    public boolean verifyOTP(String mobileNumber, String otp) {
        String storedOtp = otpStore.get(mobileNumber);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(mobileNumber); // Remove after successful verification
            return true;
        }
        return false;
    }

    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(999999)); // 6-digit OTP
    }
}