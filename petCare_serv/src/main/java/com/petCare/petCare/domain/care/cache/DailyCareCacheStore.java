package com.petCare.petCare.domain.care.cache;

import java.time.LocalDate;
import java.util.Optional;

import com.petCare.petCare.domain.care.dto.DailyCareSummaryResponse;

public interface DailyCareCacheStore {

	Optional<DailyCareSummaryResponse> get(Long familyId, Long petId, LocalDate date);

	void put(Long familyId, Long petId, LocalDate date, DailyCareSummaryResponse summary);

	void evict(Long familyId, Long petId, LocalDate date);

	static String buildKey(Long familyId, Long petId, LocalDate date) {
		return "petcare:care:" + familyId + ":" + petId + ":" + date;
	}

}
