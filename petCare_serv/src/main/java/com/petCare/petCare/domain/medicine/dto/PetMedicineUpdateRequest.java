package com.petCare.petCare.domain.medicine.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PetMedicineUpdateRequest(
	@NotBlank(message = "약 이름을 입력해주세요.")
	@Size(max = 150, message = "약 이름은 150자 이하로 입력해주세요.")
	String name,

	@NotBlank(message = "급여 방법을 입력해주세요.")
	@Size(max = 2000, message = "급여 방법은 2000자 이하로 입력해주세요.")
	String dosageMethod,

	@Min(value = 1, message = "1일 급여 횟수는 1회 이상이어야 합니다.")
	int dailyDoseCount,

	@Min(value = 1, message = "총 처방 횟수는 1회 이상이어야 합니다.")
	int totalDoses,

	@Min(value = 0, message = "남은 투약 횟수는 0 이상이어야 합니다.")
	int remainingDoses,

	LocalDate startDate,

	boolean active
) {
}
