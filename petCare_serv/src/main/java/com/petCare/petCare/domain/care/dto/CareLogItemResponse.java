package com.petCare.petCare.domain.care.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.petCare.petCare.domain.care.entity.CareType;
import com.petCare.petCare.domain.care.entity.DailyCareLog;
import com.petCare.petCare.domain.member.entity.Member;

public record CareLogItemResponse(
	Long id,
	Long petId,
	CareType careType,
	LocalDate careDate,
	LocalDateTime performedAt,
	String memo,
	Long memberId,
	String memberNickname
) {
	public static CareLogItemResponse from(DailyCareLog log) {
		Member member = log.getMember();
		String nickname = (member != null && member.getDeletedAt() == null)
			? member.getNickname()
			: "탈퇴한 가족";

		return new CareLogItemResponse(
			log.getId(),
			log.getPet().getId(),
			log.getCareType(),
			log.getCareDate(),
			log.getPerformedAt(),
			log.getMemo(),
			member != null ? member.getId() : null,
			nickname
		);
	}
}
