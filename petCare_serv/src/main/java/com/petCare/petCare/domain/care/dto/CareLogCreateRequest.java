package com.petCare.petCare.domain.care.dto;

import java.time.LocalDateTime;

import com.petCare.petCare.domain.care.entity.CareType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CareLogCreateRequest(
	@NotNull(message = "케어 활동 종류(WALK, MEAL, SNACK)를 선택해주세요.")
	CareType careType,

	LocalDateTime performedAt,

	@Size(max = 200, message = "메모는 200자 이하로 입력해주세요.")
	String memo
) {
}
