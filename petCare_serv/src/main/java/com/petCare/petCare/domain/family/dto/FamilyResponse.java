package com.petCare.petCare.domain.family.dto;

import java.util.List;

import com.petCare.petCare.domain.family.entity.Family;

public record FamilyResponse(
	Long familyId,
	String name,
	String inviteCode,
	List<FamilyMemberSummary> members
) {

	public static FamilyResponse of(Family family, List<FamilyMemberSummary> members) {
		return new FamilyResponse(family.getId(), family.getName(), family.getInviteCode(), members);
	}

}
