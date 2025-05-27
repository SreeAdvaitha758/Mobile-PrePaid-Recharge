package com.mobilePrepaid.system.model;

import java.util.Date;

import jakarta.persistence.*;

@Entity
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriberId;
    
    private String username;
    
    private String password;
    
    @Column(unique = true)
    private String mobileNumber;
    
    private String email;
    
    private String currentPlan;
    
    @Temporal(TemporalType.DATE)
    private Date planExpiryDate;
    
    private String status; // "ACTIVE" or "INACTIVE"
    @Temporal(TemporalType.DATE)
    private Date lastRechargeDate;
    
    

    public Long getSubscriberId() {
        return subscriberId;
    }
    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }
    public String getMobileNumber() {
        return mobileNumber;
    }
    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public Date getPlanExpiryDate() {
        return planExpiryDate;
    }
    public void setPlanExpiryDate(Date planExpiryDate) {
        this.planExpiryDate = planExpiryDate;
    }
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getCurrentPlan() {
		return currentPlan;
	}
	public void setCurrentPlan(String currentPlan) {
		this.currentPlan = currentPlan;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getLastRechargeDate() {
		return lastRechargeDate;
	}
	public void setLastRechargeDate(Date lastRechargeDate) {
		this.lastRechargeDate = lastRechargeDate;
	}
}
