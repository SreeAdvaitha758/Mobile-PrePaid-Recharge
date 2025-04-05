package com.mobilePrepaid.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mobilePrepaid.system.model.RechargePlan;

@Repository
public interface RechargePlanRepository extends JpaRepository<RechargePlan, Long> {
	List<RechargePlan> findByIsActiveTrue();
	@Query("SELECT r FROM RechargePlan r WHERE r.category.categoryId = :categoryId")
    List<RechargePlan> findByCategoryId(@Param("categoryId") Long categoryId);
}
