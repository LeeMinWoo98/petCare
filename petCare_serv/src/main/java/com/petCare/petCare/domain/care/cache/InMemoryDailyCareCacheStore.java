package com.petCare.petCare.domain.care.cache;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.care.dto.DailyCareSummaryResponse;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryDailyCareCacheStore implements DailyCareCacheStore {

	private final Map<String, DailyCareSummaryResponse> store = new ConcurrentHashMap<>();

	@Override
	public Optional<DailyCareSummaryResponse> get(Long familyId, Long petId, LocalDate date) {
		return Optional.ofNullable(store.get(DailyCareCacheStore.buildKey(familyId, petId, date)));
	}

	@Override
	public void put(Long familyId, Long petId, LocalDate date, DailyCareSummaryResponse summary) {
		store.put(DailyCareCacheStore.buildKey(familyId, petId, date), summary);
	}

	@Override
	public void evict(Long familyId, Long petId, LocalDate date) {
		store.remove(DailyCareCacheStore.buildKey(familyId, petId, date));
	}

	public void clearAll() {
		store.clear();
	}

}
