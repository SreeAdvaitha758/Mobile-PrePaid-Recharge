package com.mobilePrepaid.system.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mobilePrepaid.system.enums.PaymentMode;
import com.mobilePrepaid.system.enums.TransactionStatus;
import com.mobilePrepaid.system.model.PaymentTransaction;
import com.mobilePrepaid.system.model.RechargePlan;
import com.mobilePrepaid.system.model.Subscriber;
import com.mobilePrepaid.system.repository.PaymentTransactionRepository;
import com.mobilePrepaid.system.repository.RechargePlanRepository;
import com.mobilePrepaid.system.repository.SubscriberRepository;

@Service
public class SubscriberService {
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    @Autowired
    private RechargePlanRepository rechargePlanRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private EmailService emailService;

    public Subscriber registerSubscriber(Subscriber subscriber) {
        subscriber.setStatus("ACTIVE"); // Default status
        return subscriberRepository.save(subscriber);
    }
    
    public Subscriber login(String mobileNumber) {
        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber);
        if (subscriber != null && "ACTIVE".equals(subscriber.getStatus())) {
            return subscriber;
        }
        return null;
    }

    public List<RechargePlan> getActivePlans() {
        return rechargePlanRepository.findByIsActiveTrue();
    }

    public PaymentTransaction processPayment(Long subscriberId, Long planId, PaymentMode paymentMode) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new RuntimeException("Subscriber not found"));
        RechargePlan plan = rechargePlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setSubscriber(subscriber);
        transaction.setPlan(plan);
        transaction.setAmount(plan.getPrice());
        transaction.setTransactionDate(new Date());
        transaction.setPaymentMode(paymentMode);
        transaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);

        // Update subscriber details
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, plan.getValidityDays());
        subscriber.setPlanExpiryDate(cal.getTime());
        subscriber.setCurrentPlan(plan.getPlanName());
        subscriber.setLastRechargeDate(new Date());
        subscriberRepository.save(subscriber);

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // Send confirmation email
        String subject = "Payment Confirmation";
        String body = String.format(
                "Dear %s,\n\nYour payment has been successfully processed.\n\nTransaction ID: %d\nPlan: %s\nPrice: ₹%.2f\nValidity: %d days\nData: %.2f GB\nCalls: %s\nSMS: %d\n\nThank you for choosing Mobi-Comm!",
                subscriber.getUsername(), savedTransaction.getTransaction_id(), plan.getPlanName(), plan.getPrice(),
                plan.getValidityDays(), plan.getData(), plan.getCalls(), plan.getSms());
        emailService.sendEmail(subscriber.getEmail(), subject, body);

        return savedTransaction;
    }
    
}