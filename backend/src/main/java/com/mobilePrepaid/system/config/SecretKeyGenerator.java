package com.mobilePrepaid.system.config;

import java.security.SecureRandom;
import java.util.Base64;

public class SecretKeyGenerator {
    public static void main(String[] args) {
        // Generate a 256-bit (32-byte) random key
        byte[] key = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);

        // Encode to Base64 for readability
        String secretKey = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated Secret Key: " + secretKey);
    }
}