package com.mobilePrepaid.system.dto;

public class PaymentRequest {
	private Long planId;
    private Double amount;
    private String paymentMode;
    
    public PaymentRequest() {}
    public PaymentRequest(Long planId, Double amount, String paymentMode) {
        this.planId = planId;
        this.amount = amount;
        this.paymentMode = paymentMode;
    }
	public Long getPlanId() {
		return planId;
	}
	public void setPlanId(Long planId) {
		this.planId = planId;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public String getPaymentMode() {
		return paymentMode;
	}
	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}

}
