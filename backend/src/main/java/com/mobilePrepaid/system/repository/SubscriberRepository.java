package com.mobilePrepaid.system.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mobilePrepaid.system.model.Subscriber;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
	Subscriber findByMobileNumber(String mobileNumber);
    List<Subscriber> findByPlanExpiryDateBefore(Date date);
    
    @Query("SELECT s FROM Subscriber s WHERE s.lastRechargeDate < :date AND s.status = 'ACTIVE'")
    List<Subscriber> findInactiveSubscribers(@Param("date") Date date);
    
}
