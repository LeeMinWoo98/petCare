package com.petCare.petCare.domain.care.dto;

import java.time.LocalDate;
import java.util.List;

import com.petCare.petCare.domain.care.entity.CareType;

public record DailyCareSummaryResponse(
	Long petId,
	LocalDate date,
	long walkCount,
	long mealCount,
	long snackCount,
	List<CareLogItemResponse> logs
) {
	public static DailyCareSummaryResponse of(Long petId, LocalDate date, List<CareLogItemResponse> logs) {
		long walkCount = logs.stream().filter(item -> item.careType() == CareType.WALK).count();
		long mealCount = logs.stream().filter(item -> item.careType() == CareType.MEAL).count();
		long snackCount = logs.stream().filter(item -> item.careType() == CareType.SNACK).count();
		return new DailyCareSummaryResponse(petId, date, walkCount, mealCount, snackCount, logs);
	}
}
