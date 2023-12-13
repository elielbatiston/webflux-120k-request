package com.fullcycle.webflux120krequest.repositories;

import com.fullcycle.webflux120krequest.models.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRepository {

	private final Database database;

	public Mono<Payment> createPayment(final String userId) {
		final Payment payment = Payment.builder()
			.id(UUID.randomUUID().toString())
			.userId(userId)
			.status(Payment.PaymentStatus.PENDING)
			.build();
		return Mono.fromCallable(() -> {
			log.info("Saving payment transaction for user {}", userId);
			return this.database.save(userId, payment);
		})
			.subscribeOn(Schedulers.boundedElastic())
			.doOnNext(next -> log.info("Payment received {}", next.getUserId()));
	}

	// A instrução abaixo faz um Mono de Optional o que não faz sentido porque o Mono já é um 0 ou N igual ao optional.
	//public Mono<Optional<Payment>> getPayment(final String userId) { }

	public Mono<Payment> getPayment(final String userId) {
		// Direfente do fromCallable o defer, deixa a execução para depois e essa execução retorna um Mono
		return Mono.defer(() -> {
			log.info("Getting payment from database - {}", userId);
			final Optional<Payment> payment = this.database.get(userId, Payment.class);
			return Mono.justOrEmpty(payment);
		})
			.subscribeOn(Schedulers.boundedElastic())
			.doOnNext(it -> log.info("Payment received - {}", userId));
	}

	public Mono<Payment> processPayment(final String key, final Payment.PaymentStatus paymentStatus) {
		log.info("On payment {} received to status {}", key, paymentStatus);
		return getPayment(key)
			.flatMap(payment -> Mono.fromCallable(() -> {
				log.info("Processing payment {} to status {}", key, paymentStatus);
				return this.database.save(key, payment.withStatus(paymentStatus));
			})
			.subscribeOn(Schedulers.boundedElastic()));
	}
}
