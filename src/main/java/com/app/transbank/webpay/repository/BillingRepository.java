package com.app.transbank.webpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.transbank.webpay.model.Billing;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long>{
	
	Billing findBillingByToken(String token);
	
	
	
}
