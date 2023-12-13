package com.fullcycle.webflux120krequest.controllers;

import com.fullcycle.webflux120krequest.models.Payment;
import com.fullcycle.webflux120krequest.publishers.PaymentPublisher;
import com.fullcycle.webflux120krequest.repositories.InMemoryDatabase;
import com.fullcycle.webflux120krequest.repositories.PaymentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

	private final PaymentRepository repository;
	private final PaymentPublisher publisher;

	@PostMapping
	public Mono<Payment> createPayment(@RequestBody final NewPaymentInput input) {
		final String userId = input.getUserId();
		log.info("Payment to be processed {}", userId);

		// Quem esta sendo observado é o this.repository.createPayment(userId)
		// Quando ele tiver um evento e ele emitir esse evento, eu vou receber no meu handler .flatMap(this.publisher::onPaymentCreate)
		// e agora quem será observado é this.publisher::onPaymentCreate (está indo para outra thread)
		// Ele não roda as 2 em paralelo, ele termina a this.repository.createPayment(userId) e depois vai pra this.publisher::onPaymentCreate
		// Por padrão, ele vai terminar tudo de this.repository.createPayment(userId) porque é um Mono e quando tiver um próximo evento desse cara
		// que no caso é esse cara this.publisher::onPaymentCreate, ele vai emitir um evento e um sinal de complete. Ou seja, o this.repository.createPayment(userId)
		// vai emitir um sinal de completou e não faz mais nada assim, o this.publisher::onPaymentCreate vai processar, emitir um sinal de evento e depois um sinal de completou
		// e ai vai para o próxima processo da pipe que é emitir o log log.info("Payment processed {}", userId)
		return this.repository.createPayment(userId)
			//.flatMap(payment -> this.publisher.onPaymentCreate(payment))
			.flatMap(this.publisher::onPaymentCreate)

			// Não quero retornar a mensagem enquanto o pagamento estiver pendente, só vou retornar quando ele estiver aprovado.
			// Então dentro do meu método eu vou fazer um pooling pra onde? Para o meu banco de dados. Não recomendado fazer isso produtivamente,
			// é mais pra demonstrar o poder do reactor.

			.flatMap(payment -> {
				// A cada 1 segundo, vou ao banco e busco o pagamento
				// verifio se está aprovado e se sim, volto para Mono
				// Basicamente estou fazendo um laço de repetição a grosso modo (é um loop) para fazer um pooling
				// que é basicamente eu ficar indo toda hora no banco buscando esse meu payment e se eu vou passar esse evento pra frente
				// quando o meu pagamento estiver aprovado. Quando passar o primeiro evento é esse cara que quero usar e converte da cadeia
				// de flux para a cadeia de mono.
				// Tudo isso é feito segurando a requisição do client porem sem bloquear outras chamadas.
				final Mono<Payment> mono = Flux.interval(Duration.ofSeconds(1))
					.doOnNext(it -> log.info("Next tick - {}", it))
					.flatMap(tick -> this.repository.getPayment(userId))
					.filter(it -> Payment.PaymentStatus.APPROVED == it.getStatus())
					.next(); // volto do flux para o mono

				return mono;
			})
			.doOnNext(next -> log.info("Payment processed {}", userId))
			.timeout(Duration.ofSeconds(20)) //Posso colocar um timeout para parar a execução se demorar
			//			.retry(3) // Caso der timeout posso fazer X retries
			.retryWhen(
				Retry.backoff(2, Duration.ofSeconds(1))
					.doAfterRetry(signal -> log.info("Execution failed... retrying.. {}", signal.totalRetries()))
			); //tenta no máximo 2x com duraçaõ de 1 segundo
	}

	@GetMapping("users")
	public Flux<Payment> findAllById(@RequestParam final String ids) {
		final List<String> _ids = Arrays.asList(ids.split(","));
		log.info("Collecting {} payments", _ids.size());
		return Flux.fromIterable(_ids)
			.flatMap(this.repository::getPayment);
	}

	@GetMapping("ids")
	public Mono<String> getIds() {
		return Mono.fromCallable(() -> String.join(",", InMemoryDatabase.DATABASE.keySet()))
			.subscribeOn(Schedulers.parallel());
	}

	@Data
	public static class NewPaymentInput {
		private String userId;
	}
}
