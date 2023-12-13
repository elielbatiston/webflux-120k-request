package com.fullcycle.webflux120krequest.publishers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullcycle.webflux120krequest.models.Payment;
import com.fullcycle.webflux120krequest.models.PubSubMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class PaymentPublisher {

	private final Sinks.Many<PubSubMessage> sink;
	private final ObjectMapper mapper;

	public Mono<Payment> onPaymentCreate(final Payment payment) {
		return Mono.fromCallable(() -> {
			final String userId = payment.getUserId();
			final String data = mapper.writeValueAsString(payment);
			return new PubSubMessage(userId, data);
		})
			.subscribeOn(Schedulers.parallel())

			// o doOnNext publica de maneira assincrona
			.doOnNext(next -> this.sink.tryEmitNext(next))

			// aqui eu digo q eu não me importo com o valor anterior da pipeline que no caso é o new PubSubMessage.
			// Então eu retorno um mono de payment (valor recebido como parametro)
			// O thenReturn é um shortcut de Mono.just(payment)
			.thenReturn(payment);
	}
}
