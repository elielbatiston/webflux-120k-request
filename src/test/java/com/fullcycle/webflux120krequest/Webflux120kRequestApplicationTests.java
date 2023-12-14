package com.fullcycle.webflux120krequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;

@SpringBootTest
class Webflux120kRequestApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	public void test1() {

		//mais recomendada
		final Mono<String> stringMono = Mono.fromCallable(() -> "OI")
			.subscribeOn(Schedulers.parallel());

		StepVerifier.create(stringMono)
			.expectNext("OI")
			.expectComplete()
			.verify();
	}

	@Test
	public void test2() {
		final String resultado = Mono.fromCallable(() -> "OI")
			.subscribeOn(Schedulers.parallel())
			.block(Duration.ofSeconds(2));

		Assertions.assertEquals("OI", resultado);
	}
}
