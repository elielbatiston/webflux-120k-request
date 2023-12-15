package com.fullcycle.webflux120krequest.publishers;

import com.fullcycle.webflux120krequest.models.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class PaymentProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public Mono<Payment> onPaymentCreate(final Payment payment) {
		return Mono.fromCallable(() -> {
			kafkaTemplate.send("payment", payment);
			return payment;
		})
			.subscribeOn(Schedulers.parallel())
			.thenReturn(payment);
	}
}
