package com.mobilePrepaid.system.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mobilePrepaid.system.model.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
	@Query("SELECT pt FROM PaymentTransaction pt WHERE pt.subscriber.id = :subscriberId")
	List<PaymentTransaction> findBySubscriberId(@Param("subscriberId") Long subscriberId);
}