package com.mobilePrepaid.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.twilio.Twilio;

import jakarta.annotation.PostConstruct;
@Component
public class TwilioConfig {
	
	@Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        if (accountSid == null || authToken == null) {
            throw new IllegalStateException("Twilio Account SID or Auth Token is not set. Check application.properties.");
        }
        Twilio.init(accountSid, authToken);
        System.out.println("Twilio initialized with Account SID: " + accountSid);
    }
}
