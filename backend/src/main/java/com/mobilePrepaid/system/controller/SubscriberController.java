package com.mobilePrepaid.system.controller;

import com.mobilePrepaid.system.config.JwtUtil;
import com.mobilePrepaid.system.dto.EmailRequest;
import com.mobilePrepaid.system.dto.PaymentRequest;
import com.mobilePrepaid.system.dto.RechargeRequest;
import com.mobilePrepaid.system.enums.PaymentMode;
import com.mobilePrepaid.system.enums.TransactionStatus;
import com.mobilePrepaid.system.model.PaymentTransaction;
import com.mobilePrepaid.system.model.RechargePlan;
import com.mobilePrepaid.system.model.Subscriber;
import com.mobilePrepaid.system.repository.PaymentTransactionRepository;
import com.mobilePrepaid.system.repository.RechargePlanRepository;
import com.mobilePrepaid.system.repository.SubscriberRepository;
import com.mobilePrepaid.system.service.SubscriberService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://127.0.0.1:5500"})
@RequestMapping("/api/subscriber")
public class SubscriberController {
    private static final Logger logger = LoggerFactory.getLogger(SubscriberController.class);

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private RechargePlanRepository rechargePlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RazorpayClient razorpayClient;
    
    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    @PostMapping("/register")
    public ResponseEntity<Subscriber> register(@RequestBody Subscriber subscriber) {
        try {
            subscriber.setPassword(passwordEncoder.encode(subscriber.getPassword()));
            Subscriber registeredSubscriber = subscriberService.registerSubscriber(subscriber);
            return new ResponseEntity<>(registeredSubscriber, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
   
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String mobileNumber = loginRequest.get("mobileNumber");
        System.out.println("Login request for mobile number: " + mobileNumber);
        Subscriber subscriber = subscriberService.login(mobileNumber);
        if (subscriber != null) {
            System.out.println("Subscriber found: " + subscriber);
            String token = jwtUtil.generateToken(mobileNumber, "SUBSCRIBER");
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", "SUBSCRIBER");
            response.put("id", subscriber.getSubscriberId() != null ? subscriber.getSubscriberId().toString() : null);
            response.put("mobileNumber", subscriber.getMobileNumber());
            response.put("username", subscriber.getUsername());
            response.put("email", subscriber.getEmail());
            System.out.println("Login response: " + response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        System.out.println("Subscriber not found for mobile number: " + mobileNumber);
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    
    @GetMapping("/plans")
    public ResponseEntity<List<RechargePlan>> getPlans() {
        List<RechargePlan> plans = subscriberService.getActivePlans();
        return plans != null && !plans.isEmpty() ? new ResponseEntity<>(plans, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{subscriberId}/create-order")
    public ResponseEntity<Map<String, String>> createOrder(@PathVariable Long subscriberId, @RequestBody PaymentRequest paymentRequest) {
        try {
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));
            RechargePlan plan = rechargePlanRepository.findById(paymentRequest.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (paymentRequest.getAmount() * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + subscriberId + "_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            logger.info("Razorpay order created: {}", order.toString());

            Map<String, String> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", String.valueOf(paymentRequest.getAmount()));
            response.put("currency", "INR");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RazorpayException e) {
            logger.error("Razorpay order creation failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{subscriberId}/recharge")
    @Transactional
    public ResponseEntity<Map<String, String>> processRecharge(
            @PathVariable Long subscriberId,
            @RequestBody RechargeRequest paymentResponse) {
        Map<String, String> response = new HashMap<>();
        try {
            // Log the entire request body for debugging
            logger.info("Received recharge request for subscriberId: {}, requestBody: {}", subscriberId, paymentResponse);

            String razorpayOrderId = paymentResponse.getRazorpayOrderId();
            String razorpayPaymentId = paymentResponse.getRazorpayPaymentId();
            String razorpaySignature = paymentResponse.getRazorpaySignature();
            Long planId = paymentResponse.getPlanId();
            Double amount = paymentResponse.getAmount();
            String mobileNumber = paymentResponse.getMobileNumber();
            String email = paymentResponse.getEmail();

            if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
                logger.error("Missing Razorpay response parameters: razorpay_order_id={}, razorpay_payment_id={}, razorpay_signature={}",
                        razorpayOrderId, razorpayPaymentId, razorpaySignature);
                response.put("error", "Missing Razorpay response parameters");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            logger.info("Verifying Razorpay payment: orderId={}, paymentId={}", razorpayOrderId, razorpayPaymentId);
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            boolean isSignatureValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (!isSignatureValid) {
                logger.error("Payment signature verification failed");
                response.put("error", "Payment signature verification failed");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            logger.info("Razorpay payment verified successfully");

            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found with ID: " + subscriberId));
            RechargePlan plan = rechargePlanRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));
            logger.info("Subscriber and plan found: subscriberId={}, planId={}", subscriberId, planId);

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setSubscriber(subscriber);
            transaction.setPlan(plan);
            transaction.setTransactionDate(new Date());
            transaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);
            transaction.setAmount(amount);
            transaction.setPaymentMode(PaymentMode.RAZORPAY);
            logger.info("Saving payment transaction for paymentId={}", razorpayPaymentId);
            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
            logger.info("Payment transaction saved successfully: transactionId={}", savedTransaction.getTransaction_id());

            logger.info("Updating subscriber details for subscriberId={}", subscriberId);
            subscriber.setLastRechargeDate(new Date());
            subscriber.setCurrentPlan(plan.getPlanName());
            subscriber.setPlanExpiryDate(calculateExpiryDate(new Date(), plan.getValidityDays()));
            subscriberRepository.save(subscriber);
            logger.info("Subscriber details updated successfully");

            logger.info("Sending confirmation email to: {}", email);
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            EmailRequest emailRequest = new EmailRequest(
                    email,
                    plan.getPrice().toString(),
                    plan.getValidityDays().toString(),
                    plan.getData().toString(),
                    plan.getCalls(),
                    plan.getSms().toString(),
                    subscriber.getUsername(),
                    savedTransaction.getTransaction_id().toString()
            );
            HttpEntity<EmailRequest> request = new HttpEntity<>(emailRequest, headers);
            ResponseEntity<String> emailResponse = restTemplate.postForEntity(
                    "http://localhost:8081/api/send-email", request, String.class
            );

            if (!emailResponse.getStatusCode().is2xxSuccessful()) {
                logger.warn("Email sending failed with status: {}", emailResponse.getStatusCode());
            } else {
                logger.info("Email sent successfully to: {}", email);
            }

            response.put("transaction_id", savedTransaction.getTransaction_id().toString());
            logger.info("Recharge successful for subscriberId: {}, transactionId: {}", 
                    subscriberId, savedTransaction.getTransaction_id());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NumberFormatException e) {
            logger.error("Invalid number format in payment response: {}", e.getMessage());
            response.put("error", "Invalid number format: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (RazorpayException e) {
            logger.error("Razorpay verification failed: {}", e.getMessage());
            response.put("error", "Razorpay verification failed: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            logger.error("Recharge processing failed: {}", e.getMessage());
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Unexpected error during recharge: {}", e.getMessage(), e);
            response.put("error", "Unexpected error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{subscriberId}/history")
    public ResponseEntity<List<PaymentTransaction>> getSubscriberHistory(@PathVariable Long subscriberId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findBySubscriberId(subscriberId);
        return transactions != null && !transactions.isEmpty() ? new ResponseEntity<>(transactions, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private Date calculateExpiryDate(Date startDate, int validityDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DAY_OF_MONTH, validityDays);
        return calendar.getTime();
    }
    
    private String hmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] result = mac.doFinal(data.getBytes("UTF-8"));
        return bytesToHex(result);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}