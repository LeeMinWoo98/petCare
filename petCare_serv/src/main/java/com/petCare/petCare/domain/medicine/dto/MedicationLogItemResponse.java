package com.petCare.petCare.domain.medicine.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.petCare.petCare.domain.medicine.entity.MedicationLog;
import com.petCare.petCare.domain.member.entity.Member;

public record MedicationLogItemResponse(
	Long id,
	Long petMedicineId,
	LocalDate medicatedDate,
	LocalDateTime medicatedAt,
	String memo,
	Long memberId,
	String memberNickname
) {
	public static MedicationLogItemResponse from(MedicationLog log) {
		Member member = log.getMember();
		String nickname = (member != null && member.getDeletedAt() == null)
			? member.getNickname()
			: "탈퇴한 가족";

		return new MedicationLogItemResponse(
			log.getId(),
			log.getPetMedicine().getId(),
			log.getMedicatedDate(),
			log.getMedicatedAt(),
			log.getMemo(),
			member != null ? member.getId() : null,
			nickname
		);
	}
}
