package com.petCare.petCare.domain.realtime.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.petCare.petCare.domain.care.entity.CareType;

public record FamilyRealtimeEvent(
	String eventType,
	Long familyId,
	Long petId,
	String careDate,
	Long actorMemberId,
	String actorNickname,
	String careType,
	Long logId,
	Long petMedicineId,
	String medicineName,
	String occurredAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static FamilyRealtimeEvent careLogChanged(
		String eventType,
		Long familyId,
		Long petId,
		LocalDate careDate,
		Long actorMemberId,
		String actorNickname,
		CareType careType,
		Long logId
	) {
		return new FamilyRealtimeEvent(
			eventType,
			familyId,
			petId,
			careDate.toString(),
			actorMemberId,
			actorNickname,
			careType.name(),
			logId,
			null,
			null,
			ZonedDateTime.now(KST).toOffsetDateTime().toString()
		);
	}

	public static FamilyRealtimeEvent medicationLogChanged(
		String eventType,
		Long familyId,
		Long petId,
		LocalDate medicatedDate,
		Long actorMemberId,
		String actorNickname,
		Long petMedicineId,
		String medicineName,
		Long logId
	) {
		return new FamilyRealtimeEvent(
			eventType,
			familyId,
			petId,
			medicatedDate.toString(),
			actorMemberId,
			actorNickname,
			"MEDICATION",
			logId,
			petMedicineId,
			medicineName,
			ZonedDateTime.now(KST).toOffsetDateTime().toString()
		);
	}
}
