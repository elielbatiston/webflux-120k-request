package com.fullcycle.webflux120krequest.controllers;

import com.fullcycle.webflux120krequest.models.Payment;
import com.fullcycle.webflux120krequest.publishers.PaymentProducer;
import com.fullcycle.webflux120krequest.services.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

	public static final int CONCURRENCY = 2000;
	public static final String COMMA_SEPARATOR = ",";
	private final PaymentService service;
	private final PaymentProducer publisher;

	@PostMapping
	public Mono<Payment> createPayment(@RequestBody final NewPaymentInput input) {
		final String userId = input.getUserId();
		log.info("Payment to be processed {}", userId);

		return this.service.createPayment(userId)
			.flatMap(this.publisher::onPaymentCreate)
			.doOnNext(next -> log.info("Payment processed {}", userId))
			.retryWhen(
				Retry.backoff(2, Duration.ofSeconds(1))
					.doAfterRetry(signal -> log.info("Execution failed... retrying.. {}", signal.totalRetries()))
			);
	}

	@GetMapping("users")
	public Flux<Payment> findAllById(@RequestParam final String ids) {
		final List<String> _ids = Arrays.asList(ids.split(COMMA_SEPARATOR));
		log.info("Collecting {} payments", _ids.size());
		return Flux.fromIterable(_ids)
			.flatMap(this.service::getPayment, CONCURRENCY);
	}

	@GetMapping("ids")
	public Mono<String> getIds() {
		return this.service.getIds();
	}

	@Data
	public static class NewPaymentInput {
		private String userId;
	}
}
