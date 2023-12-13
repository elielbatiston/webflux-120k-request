package com.fullcycle.webflux120krequest.listeners;

import com.fullcycle.webflux120krequest.models.Payment;
import com.fullcycle.webflux120krequest.models.PubSubMessage;
import com.fullcycle.webflux120krequest.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
// InitializingBean diz para o Spring que depois que ele injetar as dependencias no Bean
// Você chama o método afterPropertiesSet. Então é só para o spring chamar o hook pra gente
public class PaymentListeners implements InitializingBean {

	private final Sinks.Many<PubSubMessage> sink;
	private final PaymentRepository repository;

	@Override
	public void afterPropertiesSet() {
		this.sink.asFlux()
			.delayElements(Duration.ofSeconds(2))
			.subscribe(
				next -> {
					log.info("On next message - {}", next.getKey());
					this.repository.processPayment(next.getKey(), Payment.PaymentStatus.APPROVED)
						.doOnNext(it -> log.info("Payment processed on listener"))
						.subscribe();
				},
				error -> {
					log.error("On pub-sub listener observe error", error);
				},
				() -> { // onComplete
					log.info("On pub-sub listener complete"); // nunca vai cair aqui pq não estamos finalizando o pub-sub
				}
			);
	}
}
