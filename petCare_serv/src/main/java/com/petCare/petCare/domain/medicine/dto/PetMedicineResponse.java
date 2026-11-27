package com.petCare.petCare.domain.medicine.dto;

import java.time.LocalDate;
import java.util.List;

import com.petCare.petCare.domain.medicine.entity.MedicineSourceType;
import com.petCare.petCare.domain.medicine.entity.PetMedicine;

public record PetMedicineResponse(
	Long id,
	Long petId,
	Long catalogId,
	MedicineSourceType sourceType,
	String name,
	String dosageMethod,
	int dailyDoseCount,
	int totalDoses,
	int remainingDoses,
	int remainingDays,
	boolean lowStock,
	boolean depleted,
	LocalDate startDate,
	boolean active,
	int todayDoseCount,
	boolean todayCompleted,
	List<MedicationLogItemResponse> todayLogs
) {
	public static PetMedicineResponse of(PetMedicine medicine, List<MedicationLogItemResponse> todayLogs) {
		int todayCount = todayLogs.size();
		return new PetMedicineResponse(
			medicine.getId(),
			medicine.getPet().getId(),
			medicine.getCatalog() != null ? medicine.getCatalog().getId() : null,
			medicine.getSourceType(),
			medicine.getName(),
			medicine.getDosageMethod(),
			medicine.getDailyDoseCount(),
			medicine.getTotalDoses(),
			medicine.getRemainingDoses(),
			medicine.calculateRemainingDays(),
			medicine.isLowStock(),
			medicine.isDepleted(),
			medicine.getStartDate(),
			medicine.isActive(),
			todayCount,
			todayCount >= medicine.getDailyDoseCount(),
			todayLogs
		);
	}
}
