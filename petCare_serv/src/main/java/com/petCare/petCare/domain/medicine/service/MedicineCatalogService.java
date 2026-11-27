package com.petCare.petCare.domain.medicine.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.medicine.cache.MedicineSearchCacheStore;
import com.petCare.petCare.domain.medicine.dto.MedicineCatalogItemResponse;
import com.petCare.petCare.domain.medicine.entity.MedicineCatalog;
import com.petCare.petCare.domain.medicine.repository.MedicineCatalogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineCatalogService {

	private final MedicineCatalogRepository catalogRepository;
	private final MedicineSearchCacheStore searchCacheStore;

	public List<MedicineCatalogItemResponse> search(String keyword) {
		String trimmed = keyword == null ? "" : keyword.trim();

		return searchCacheStore.get(trimmed)
			.orElseGet(() -> {
				List<MedicineCatalog> found = trimmed.isEmpty()
					? catalogRepository.findTop20ByOrderByNameAsc()
					: catalogRepository
						.findTop20ByNameContainingIgnoreCaseOrManufacturerContainingIgnoreCaseOrderByNameAsc(
							trimmed,
							trimmed
						);

				List<MedicineCatalogItemResponse> results = found.stream()
					.map(MedicineCatalogItemResponse::from)
					.toList();
				searchCacheStore.put(trimmed, results);
				return results;
			});
	}

}
