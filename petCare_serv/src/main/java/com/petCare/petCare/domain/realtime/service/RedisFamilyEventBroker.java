package com.petCare.petCare.domain.realtime.service;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.realtime.dto.FamilyRealtimeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisFamilyEventBroker implements FamilyEventBroker, MessageListener {

	public static final String CHANNEL_PREFIX = "petcare:events:family:";

	private final StringRedisTemplate stringRedisTemplate;
	private final JsonMapper jsonMapper;
	private final FamilySseRegistry sseRegistry;

	@Override
	public void publish(FamilyRealtimeEvent event) {
		String channel = CHANNEL_PREFIX + event.familyId();
		try {
			String payload = jsonMapper.writeValueAsString(event);
			stringRedisTemplate.convertAndSend(channel, payload);
		} catch (Exception ex) {
			log.warn("Redis Pub/Sub publish failed for channel={}, falling back to local SSE dispatch", channel, ex);
			sseRegistry.dispatchToLocalEmitters(event);
		}
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			String payload = new String(message.getBody(), StandardCharsets.UTF_8);
			FamilyRealtimeEvent event = jsonMapper.readValue(payload, FamilyRealtimeEvent.class);
			sseRegistry.dispatchToLocalEmitters(event);
		} catch (Exception ex) {
			log.warn("Failed to deserialize family realtime event from Redis Pub/Sub", ex);
		}
	}

}
