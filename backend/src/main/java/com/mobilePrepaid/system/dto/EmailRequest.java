package com.mobilePrepaid.system.dto;

public class EmailRequest {
	private String email;
    private String price;
    private String validity;
    private String data;
    private String calls;
    private String sms;
    private String subscriberName;
    private String transaction_id;
    
    public EmailRequest() {}
    
    public EmailRequest(String email, String price, String validity, String data, 
            String calls, String sms, String subscriberName, String transaction_id) {
        this.email = email;
        this.price = price;
        this.validity = validity;
        this.data = data;
        this.calls = calls;
        this.sms = sms;
        this.subscriberName = subscriberName;
        this.transaction_id = transaction_id;
    }
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	public String getValidity() {
		return validity;
	}
	public void setValidity(String validity) {
		this.validity = validity;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getCalls() {
		return calls;
	}
	public void setCalls(String calls) {
		this.calls = calls;
	}
	public String getSms() {
		return sms;
	}
	public void setSms(String sms) {
		this.sms = sms;
	}

	public String getSubscriberName() {
		return subscriberName;
	}

	public void setSubscriberName(String subscriberName) {
		this.subscriberName = subscriberName;
	}

	public String getTransaction_id() {
		return transaction_id;
	}

	public void setTransaction_id(String transaction_id) {
		this.transaction_id = transaction_id;
	}

	

}
