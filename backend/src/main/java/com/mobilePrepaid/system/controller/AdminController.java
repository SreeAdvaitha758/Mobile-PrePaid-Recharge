package com.mobilePrepaid.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mobilePrepaid.system.config.CustomUserDetailsService;
import com.mobilePrepaid.system.config.JwtUtil;
import com.mobilePrepaid.system.config.TwilioConfig;
import com.mobilePrepaid.system.model.Admin;
import com.mobilePrepaid.system.model.Category;
import com.mobilePrepaid.system.model.PaymentTransaction;
import com.mobilePrepaid.system.model.RechargePlan;
import com.mobilePrepaid.system.model.Subscriber;
import com.mobilePrepaid.system.repository.CategoryRepository;
import com.mobilePrepaid.system.repository.PaymentTransactionRepository;
import com.mobilePrepaid.system.repository.SubscriberRepository;
import com.mobilePrepaid.system.service.AdminService;
import com.mobilePrepaid.system.service.TwilioService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.mobilePrepaid.system.dto.NotificationRequest;
import com.mobilePrepaid.system.dto.OTPRequest;
import com.mobilePrepaid.system.dto.VerifyNumberResponse; // Import the new DTO
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://127.0.0.1:5500"})
public class AdminController {
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private TwilioConfig twilioConfig;
    
    @Autowired
    private TwilioService twilioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;
    
    @PostMapping("/register")
    public ResponseEntity<Admin> register(@RequestBody Admin admin) {
        try {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            Admin registeredAdmin = adminService.registerAdmin(admin);
            return new ResponseEntity<>(registeredAdmin, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        Admin admin = adminService.login(username, password);
        if (admin != null && passwordEncoder.matches(password, admin.getPassword())) {
            String token = jwtUtil.generateToken(username, admin.getRole().name());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", admin.getRole().name());
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<RechargePlan>> getAllPlans() {
        List<RechargePlan> plans = adminService.getAllPlans();
        return plans != null && !plans.isEmpty() ? new ResponseEntity<>(plans, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories != null && !categories.isEmpty() ? new ResponseEntity<>(categories, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/plans/{planId}")
    public ResponseEntity<RechargePlan> getPlanById(@PathVariable Long planId, @RequestParam(defaultValue = "false") boolean includeInactive) {
        RechargePlan plan = adminService.getPlanById(planId, includeInactive);
        return plan != null ? new ResponseEntity<>(plan, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/expiring-subscribersPlans")
    public ResponseEntity<List<Subscriber>> getExpiringSubscribers() {
        List<Subscriber> subscribers = adminService.getExpiringPlans();
        return subscribers != null && !subscribers.isEmpty() ? new ResponseEntity<>(subscribers, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/plan")
    public ResponseEntity<RechargePlan> addPlan(@RequestBody RechargePlan plan) {
        try {
            RechargePlan savedPlan = adminService.addPlan(plan);
            return new ResponseEntity<>(savedPlan, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/plan/{planId}")
    public ResponseEntity<RechargePlan> editPlan(@PathVariable Long planId, @RequestBody RechargePlan planDetails) {
        try {
            RechargePlan updatedPlan = adminService.editPlan(planId, planDetails);
            return updatedPlan != null ? new ResponseEntity<>(updatedPlan, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/plan/{planId}/status")
    public ResponseEntity<Void> togglePlanStatus(@PathVariable Long planId, @RequestParam boolean isActive) {
        try {
            adminService.togglePlanStatus(planId, isActive);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/subscribers")
    public ResponseEntity<List<Subscriber>> getAllSubscribers() {
        List<Subscriber> subscribers = adminService.getAllSubscribers();
        return subscribers != null && !subscribers.isEmpty() ? new ResponseEntity<>(subscribers, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/subscribers/mobile")
    public ResponseEntity<List<Subscriber>> getSubscriberByMobile(@RequestParam String mobileNumber) {
        Subscriber subscriber = adminService.getSubscriberByMobile(mobileNumber);
        return subscriber != null ? new ResponseEntity<>(List.of(subscriber), HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/subscribers/{subscriberId}/history")
    public ResponseEntity<List<PaymentTransaction>> getSubscriberHistory(@PathVariable Long subscriberId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findBySubscriberId(subscriberId);
        return transactions != null && !transactions.isEmpty() ? new ResponseEntity<>(transactions, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/check-inactive")
    public ResponseEntity<String> checkInactiveSubscribers() {
        try {
            adminService.checkAndNotifyInactiveSubscribers();
            return new ResponseEntity<>("Inactive subscribers notified", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to notify inactive subscribers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/notify")
    public ResponseEntity<Map<String, Object>> notifySubscriber(@RequestBody NotificationRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String mobileNumber = request.getMobileNumber();
            String subscriberName = request.getSubscriberName();
            String planName = request.getPlanName();

            if (mobileNumber == null || subscriberName == null || planName == null) {
                System.out.println("Missing fields: mobileNumber=" + mobileNumber + ", subscriberName=" + subscriberName + ", planName=" + planName);
                response.put("status", "error");
                response.put("message", "Missing required fields");
                return ResponseEntity.badRequest().body(response);
            }

            // Ensure E.164 format
            if (!mobileNumber.startsWith("+")) {
                mobileNumber = "+91" + mobileNumber; // Adjust country code as needed
            }

            System.out.println("Sending SMS to: " + mobileNumber + " from: " + twilioPhoneNumber);

            String messageBody = String.format("Dear %s, your %s is going to expire within 3 days.", subscriberName, planName);

            Message message = Message.creator(
                    new PhoneNumber(mobileNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    messageBody
            ).create();

            response.put("status", "success");
            response.put("message", "SMS sent successfully with SID: " + message.getSid());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to send SMS: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    

    @PostMapping("/verifyNumber")
    public ResponseEntity<Map<String, Object>> verifyMobileNumber(@RequestBody Map<String, String> request) {
        String mobileNumber = request.get("mobileNumber");
        logger.info("Verifying mobile number: {}", mobileNumber);

        Subscriber subscriber = adminService.getSubscriberByMobile(mobileNumber);
        if (subscriber == null || !"ACTIVE".equals(subscriber.getStatus())) {
            logger.warn("Subscriber not found or inactive for mobile number: {}", mobileNumber);
            return ResponseEntity.badRequest().body(Map.of("error", "This number is not registered or inactive."));
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(mobileNumber, "SUBSCRIBER");

        logger.info("Mobile number verified successfully: {}", mobileNumber);
        // Return subscriber details and token
        Map<String, Object> response = new HashMap<>();
        response.put("subscriberId", subscriber.getSubscriberId());
        response.put("token", token);
        response.put("username", subscriber.getUsername());
        response.put("email", subscriber.getEmail());
        return ResponseEntity.ok(response);
    }
    @PostMapping("/sendOTP")
    public ResponseEntity<Map<String, String>> sendOTP(@RequestBody OTPRequest request, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
            if (token == null || !jwtUtil.validateToken(token, userDetailsService.loadUserByUsername(request.getMobileNumber()))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized request"));
            }
            Subscriber subscriber = adminService.getSubscriberByMobile(request.getMobileNumber());
            if (subscriber == null || !"ACTIVE".equals(subscriber.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Number not registered or inactive"));
            }
            twilioService.sendOTP(request.getMobileNumber());
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verifyOTP")
    public ResponseEntity<Map<String, Object>> verifyOTP(@RequestBody OTPRequest request, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
            if (token == null || !jwtUtil.validateToken(token, userDetailsService.loadUserByUsername(request.getMobileNumber()))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized request"));
            }
            boolean isValid = twilioService.verifyOTP(request.getMobileNumber(), request.getOtp());
            if (isValid) {
                Subscriber subscriber = adminService.getSubscriberByMobile(request.getMobileNumber());
                String newToken = jwtUtil.generateToken(request.getMobileNumber(), "SUBSCRIBER");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "OTP verified successfully");
                response.put("subscriberId", subscriber.getSubscriberId().toString());
                response.put("token", newToken);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid OTP"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error verifying OTP: " + e.getMessage()));
        }
    }
    @PutMapping("/subscriber/{subscriberId}/deactivate")
    public ResponseEntity<String> deactivateSubscriber(@PathVariable Long subscriberId) {
        try {
            Subscriber subscriber = adminService.getAllSubscribers().stream()
                    .filter(s -> s.getSubscriberId().equals(subscriberId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));
            subscriber.setStatus("INACTIVE");
            subscriberRepository.save(subscriber);
            return new ResponseEntity<>("Subscriber deactivated", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to deactivate subscriber", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}