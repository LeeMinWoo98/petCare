package com.petCare.petCare.domain.medicine.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.medicine.dto.MedicineCatalogItemResponse;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryMedicineSearchCacheStore implements MedicineSearchCacheStore {

	private final Map<String, List<MedicineCatalogItemResponse>> store = new ConcurrentHashMap<>();

	@Override
	public Optional<List<MedicineCatalogItemResponse>> get(String keyword) {
		return Optional.ofNullable(store.get(MedicineSearchCacheStore.buildKey(keyword)));
	}

	@Override
	public void put(String keyword, List<MedicineCatalogItemResponse> items) {
		store.put(MedicineSearchCacheStore.buildKey(keyword), List.copyOf(items));
	}

	@Override
	public void clear() {
		store.clear();
	}

}
