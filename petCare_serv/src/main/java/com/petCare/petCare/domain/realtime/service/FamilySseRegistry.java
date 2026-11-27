package com.petCare.petCare.domain.realtime.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.petCare.petCare.domain.realtime.dto.FamilyRealtimeEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FamilySseRegistry {

	private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

	private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByFamily = new ConcurrentHashMap<>();

	public SseEmitter subscribe(Long familyId) {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		CopyOnWriteArrayList<SseEmitter> list = emittersByFamily.computeIfAbsent(
			familyId,
			key -> new CopyOnWriteArrayList<>()
		);
		list.add(emitter);

		emitter.onCompletion(() -> removeEmitter(familyId, emitter));
		emitter.onTimeout(() -> {
			removeEmitter(familyId, emitter);
			emitter.complete();
		});
		emitter.onError(ex -> removeEmitter(familyId, emitter));

		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data(Map.of("familyId", familyId, "status", "CONNECTED")));
		} catch (IOException ex) {
			removeEmitter(familyId, emitter);
			emitter.completeWithError(ex);
		}

		return emitter;
	}

	public void dispatchToLocalEmitters(FamilyRealtimeEvent event) {
		List<SseEmitter> emitters = emittersByFamily.get(event.familyId());
		if (emitters == null || emitters.isEmpty()) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
					.name("family-event")
					.data(event));
			} catch (IOException | IllegalStateException ex) {
				log.debug("SSE send failed for familyId={}, removing emitter", event.familyId());
				removeEmitter(event.familyId(), emitter);
			}
		}
	}

	public int activeConnectionCount(Long familyId) {
		List<SseEmitter> emitters = emittersByFamily.get(familyId);
		return emitters == null ? 0 : emitters.size();
	}

	@Scheduled(fixedDelay = 25000)
	public void sendHeartbeat() {
		emittersByFamily.forEach((familyId, emitters) -> {
			for (SseEmitter emitter : emitters) {
				try {
					emitter.send(SseEmitter.event().comment("heartbeat"));
				} catch (IOException | IllegalStateException ex) {
					removeEmitter(familyId, emitter);
				}
			}
		});
	}

	private void removeEmitter(Long familyId, SseEmitter emitter) {
		CopyOnWriteArrayList<SseEmitter> list = emittersByFamily.get(familyId);
		if (list != null) {
			list.remove(emitter);
			if (list.isEmpty()) {
				emittersByFamily.remove(familyId, list);
			}
		}
	}

}
