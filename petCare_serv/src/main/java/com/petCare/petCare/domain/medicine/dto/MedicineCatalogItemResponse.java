package com.petCare.petCare.domain.medicine.dto;

import com.petCare.petCare.domain.medicine.entity.MedicineCatalog;

public record MedicineCatalogItemResponse(
	Long id,
	String externalCode,
	String name,
	String manufacturer,
	String efficacy,
	String dosageGuide,
	String precautions
) {
	public static MedicineCatalogItemResponse from(MedicineCatalog catalog) {
		return new MedicineCatalogItemResponse(
			catalog.getId(),
			catalog.getExternalCode(),
			catalog.getName(),
			catalog.getManufacturer(),
			catalog.getEfficacy(),
			catalog.getDosageGuide(),
			catalog.getPrecautions()
		);
	}
}
