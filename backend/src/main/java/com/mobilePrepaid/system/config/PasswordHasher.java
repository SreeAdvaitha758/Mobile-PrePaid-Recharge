package com.mobilePrepaid.system.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("adminPass");
        System.out.println("Hashed Password: " + hashedPassword);
    }
}