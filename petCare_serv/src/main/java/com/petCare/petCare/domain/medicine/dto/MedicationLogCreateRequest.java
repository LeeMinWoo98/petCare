package com.petCare.petCare.domain.medicine.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

public record MedicationLogCreateRequest(
	LocalDateTime medicatedAt,

	@Size(max = 200, message = "메모는 200자 이하로 입력해주세요.")
	String memo
) {
}
