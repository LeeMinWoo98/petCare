package com.petCare.petCare.domain.realtime.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.realtime.dto.FamilyRealtimeEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryFamilyEventBroker implements FamilyEventBroker {

	private final FamilySseRegistry sseRegistry;

	@Override
	public void publish(FamilyRealtimeEvent event) {
		sseRegistry.dispatchToLocalEmitters(event);
	}

}
