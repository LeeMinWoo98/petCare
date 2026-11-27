package com.petCare.petCare.domain.auth.dto;

import com.petCare.petCare.domain.family.entity.Family;
import com.petCare.petCare.domain.member.entity.Member;

public record AuthResponse(
	Long memberId,
	String email,
	String nickname,
	String role,
	boolean familyOwner,
	Long familyId,
	String familyName,
	String inviteCode
) {

	public static AuthResponse from(Member member) {
		Family family = member.getFamily();
		return new AuthResponse(
			member.getId(),
			member.getEmail(),
			member.getNickname(),
			member.getRole().name(),
			member.isFamilyOwner(),
			family != null ? family.getId() : null,
			family != null ? family.getName() : null,
			family != null ? family.getInviteCode() : null
		);
	}

}
