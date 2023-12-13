package com.fullcycle.webflux120krequest.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class InMemoryDatabase implements Database {

	public static final Map<String, String> DATABASE = new ConcurrentHashMap<>();

	private final ObjectMapper mapper;

	@Override
	@SneakyThrows
	public <T> T save(final String key, final T value) {
		final var data = this.mapper.writeValueAsString(value);
		DATABASE.put(key, data);
		sleep(30); //simulando latencia real
		return value;
	}

	@Override
	public <T> Optional<T> get(final String key, final Class<T> clazz) {
		final String json = DATABASE.get(key);
		sleep(15); //simulando latencia real
		return Optional.ofNullable(json)
			.map(data -> {
				try {
					return mapper.readValue(data, clazz);
				} catch (final JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			});
	}

	private void sleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}
