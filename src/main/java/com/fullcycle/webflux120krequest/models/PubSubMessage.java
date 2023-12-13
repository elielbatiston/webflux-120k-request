package com.fullcycle.webflux120krequest.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PubSubMessage {
	String key;
	String value;
}
