package com.petCare.petCare.domain.medicine.cache;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.petCare.petCare.domain.medicine.dto.MedicineCatalogItemResponse;

public interface MedicineSearchCacheStore {

	Optional<List<MedicineCatalogItemResponse>> get(String keyword);

	void put(String keyword, List<MedicineCatalogItemResponse> items);

	void clear();

	static String buildKey(String keyword) {
		String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
		return "petcare:med:search:" + (normalized.isEmpty() ? "_all_" : normalized);
	}

}
