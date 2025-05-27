package com.mobilePrepaid.system.service;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mobilePrepaid.system.enums.Role;
import com.mobilePrepaid.system.enums.TransactionStatus;
import com.mobilePrepaid.system.model.Admin;
import com.mobilePrepaid.system.model.Category;
import com.mobilePrepaid.system.model.PaymentTransaction;
import com.mobilePrepaid.system.model.RechargePlan;
import com.mobilePrepaid.system.model.Subscriber;
import com.mobilePrepaid.system.repository.AdminRepository;
import com.mobilePrepaid.system.repository.CategoryRepository;
import com.mobilePrepaid.system.repository.PaymentTransactionRepository;
import com.mobilePrepaid.system.repository.RechargePlanRepository;
import com.mobilePrepaid.system.repository.SubscriberRepository;

@Service
public class AdminService {
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    public SubscriberRepository subscriberRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    
    @Autowired
    private RechargePlanRepository rechargePlanRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Admin registerAdmin(Admin admin) {
        admin.setRole(Role.ADMIN);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }
    
    public Admin login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username);
        if (admin != null && passwordEncoder.matches(password, admin.getPassword())) {
            return admin;
        }
        return null;
    }

    public List<Subscriber> getExpiringPlans() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 3);
        List<Subscriber> expiring = subscriberRepository.findByPlanExpiryDateBefore(cal.getTime());
        expiring.forEach(sub -> {
            PaymentTransaction latest = paymentTransactionRepository
                .findBySubscriberId(sub.getSubscriberId())
                .stream()
                .filter(t -> t.getTransactionStatus() == TransactionStatus.SUCCESSFUL)
                .max(Comparator.comparing(PaymentTransaction::getTransactionDate))
                .orElse(null);
            if (latest != null) {
                sub.setCurrentPlan(latest.getPlan().getPlanName());
            }
        });
        return expiring;
    }
    
    public RechargePlan addPlan(RechargePlan plan) {
        Category category = categoryRepository.findById(plan.getCategory().getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category not found"));
        plan.setCategory(category);
        return rechargePlanRepository.save(plan);
    }
    
    public RechargePlan editPlan(Long planId, RechargePlan planDetails) {
        RechargePlan existingPlan = rechargePlanRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Plan not found with id: " + planId));
        
        existingPlan.setPlanName(planDetails.getPlanName());
        existingPlan.setPrice(planDetails.getPrice());
        existingPlan.setValidityDays(planDetails.getValidityDays());
        existingPlan.setIsActive(planDetails.getIsActive());
        existingPlan.setData(planDetails.getData());
        existingPlan.setCalls(planDetails.getCalls());
        existingPlan.setSms(planDetails.getSms());
        existingPlan.setDescription(planDetails.getDescription());
        existingPlan.setCategory(planDetails.getCategory());
        
        return rechargePlanRepository.save(existingPlan);
    }
    
    public RechargePlan getPlanById(Long planId, boolean includeInactive) {
        return rechargePlanRepository.findById(planId)
                .filter(plan -> includeInactive || plan.getIsActive())
                .orElse(null);
    }

    public void togglePlanStatus(Long planId, boolean isActive) {
        RechargePlan plan = rechargePlanRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Plan not found"));
        plan.setIsActive(isActive);
        rechargePlanRepository.save(plan);
    }
    
    public List<RechargePlan> getAllPlans() {
        return rechargePlanRepository.findAll();
    }
    
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }

    public Subscriber getSubscriberByMobile(String mobileNumber) {
        return subscriberRepository.findByMobileNumber(mobileNumber);
    }
    
    public void notifyExpiringSubscriber(String mobileNumber) {
        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber);
        if (subscriber != null) {
            String subject = "Plan Expiry Reminder";
            String body = String.format("Dear %s,\n\nYour plan (%s) is expiring soon on %s. Please recharge to continue services.\n\nRegards,\nMobi-Comm",
                    subscriber.getUsername(), subscriber.getCurrentPlan(), subscriber.getPlanExpiryDate());
            emailService.sendEmail(subscriber.getEmail(), subject, body);
        }
    }
    
    public void checkAndNotifyInactiveSubscribers() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        List<Subscriber> inactiveSubscribers = subscriberRepository.findInactiveSubscribers(cal.getTime());
        for (Subscriber subscriber : inactiveSubscribers) {
            String subject = "Inactivity Notice";
            String body = String.format("Dear %s,\n\nYou haven’t recharged in over a year. Please recharge within 30 days to avoid deactivation.\n\nRegards,\nMobi-Comm",
                    subscriber.getUsername());
            emailService.sendEmail(subscriber.getEmail(), subject, body);

            new Thread(() -> {
                try {
                    Thread.sleep(30 * 24 * 60 * 60 * 1000L);
                    deactivateIfNoResponse(subscriber.getSubscriberId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void deactivateIfNoResponse(Long subscriberId) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new RuntimeException("Subscriber not found"));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        if (subscriber.getLastRechargeDate() != null && subscriber.getLastRechargeDate().before(cal.getTime())) {
            subscriber.setStatus("INACTIVE");
            subscriberRepository.save(subscriber);
        }
    }
}