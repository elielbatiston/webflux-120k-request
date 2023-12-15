package com.fullcycle.webflux120krequest.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@Table(name = "payment")
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

	@Id
	String id;

	String userId;

	PaymentStatus status;

	public enum PaymentStatus {
		PENDING, APPROVED
	}
}
