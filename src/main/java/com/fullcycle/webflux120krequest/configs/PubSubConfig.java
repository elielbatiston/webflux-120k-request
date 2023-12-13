package com.fullcycle.webflux120krequest.configs;

import com.fullcycle.webflux120krequest.models.PubSubMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class PubSubConfig {

	// Sink é igual a torneira. É o cara que vou usar para publicar uma mensagem, ele é o publisher
	@Bean
	public Sinks.Many<PubSubMessage> sink() {
		// Pode enviar e receber vários de uma unica vez.
		// Quando o consumidor estiver lento pedindo por exemplo de 20 em 20 mas estou produzindo de 100 em 100,
		// então ele vai bufferizar pra mim ou seja, ele vai pegar as mensagens produzidas e acumular numa fila
		// para não matar a outra máquina
		return Sinks.many().multicast()
			.onBackpressureBuffer(1000);
	}
}
