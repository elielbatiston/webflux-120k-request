package com.fullcycle.webflux120krequest.services;

import com.fullcycle.webflux120krequest.models.Payment;
import com.fullcycle.webflux120krequest.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

	public static final String COMMA_SEPARATOR = ",";
	private final PaymentRepository repository;

	public Mono<Payment> createPayment(final String userId) {
		final Payment payment = Payment.builder()
			.id(UUID.randomUUID().toString())
			.userId(userId)
			.status(Payment.PaymentStatus.PENDING)
			.build();
		return Mono.fromCallable(() -> {
				log.info("Saving payment transaction for user {}", userId);
				return this.repository.save(payment);
			})
			.subscribeOn(Schedulers.boundedElastic())
			.doOnNext(next -> log.info("Payment received {}", next.getUserId()));
	}

	public Mono<Payment> getPayment(final String userId) {
		return Mono.defer(() -> {
			log.info("Getting payment from database - {}", userId);
			final Optional<Payment> payment = this.repository.findByUserId(userId);
			return Mono.justOrEmpty(payment);
		})
			.subscribeOn(Schedulers.boundedElastic())
			.doOnNext(it -> log.info("Payment received - {}", userId));
	}

	public Mono<Payment> processPayment(final String userId, final Payment.PaymentStatus paymentStatus) {
		log.info("On payment {} received to status {}", userId, paymentStatus);
		return getPayment(userId)
			.flatMap(payment -> Mono.fromCallable(() -> {
				log.info("Processing payment {} to status {}", userId, paymentStatus);
				payment.setStatus(paymentStatus);
				return this.repository.save(payment);
			})
				.subscribeOn(Schedulers.boundedElastic()));
	}

	public Mono<String> getIds() {
		return Mono.fromCallable(this.repository::findAll)
			.subscribeOn(Schedulers.boundedElastic())
			.flatMap(payments -> {
				final String ids = payments.stream()
					.map(Payment::getId)
					.collect(Collectors.joining(COMMA_SEPARATOR));
				return Mono.just(ids);
			});
	}
}
