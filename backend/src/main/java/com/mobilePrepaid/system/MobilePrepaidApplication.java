package com.mobilePrepaid.system;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.mobilePrepaid.system.enums.Role;
import com.mobilePrepaid.system.model.Admin;
import com.mobilePrepaid.system.model.Category;
import com.mobilePrepaid.system.model.RechargePlan;
import com.mobilePrepaid.system.model.Subscriber;
import com.mobilePrepaid.system.repository.AdminRepository;
import com.mobilePrepaid.system.repository.CategoryRepository;
import com.mobilePrepaid.system.repository.RechargePlanRepository;
import com.mobilePrepaid.system.repository.SubscriberRepository;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@ComponentScan(basePackages = "com.mobilePrepaid.system")
public class MobilePrepaidApplication {


	public static void main(String[] args) {
		SpringApplication.run(MobilePrepaidApplication.class, args);
	}
	

}
