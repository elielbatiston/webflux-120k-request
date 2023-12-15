package com.fullcycle.webflux120krequest.listeners;

import com.fullcycle.webflux120krequest.models.Payment;
import com.fullcycle.webflux120krequest.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentListeners {

	private final PaymentService service;

	public PaymentListeners(final PaymentService service) {
		this.service = service;
	}

	@KafkaListener(topics = "payment", groupId = "paymentEvents")
	public void consume(final @Payload Payment payment) {
		log.info("On next message - {}", payment.getUserId());
		this.service.processPayment(payment.getUserId(), Payment.PaymentStatus.APPROVED)
			.doOnNext(it -> log.info("Payment processed on listener"))
			.subscribe();
	}
}
