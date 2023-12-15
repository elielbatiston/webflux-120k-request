package com.fullcycle.webflux120krequest.repositories;

import com.fullcycle.webflux120krequest.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

	Optional<Payment> findByUserId(String userId);
}
