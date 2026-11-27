package com.petCare.petCare.domain.family.dto;

import java.time.LocalDateTime;

import com.petCare.petCare.domain.member.entity.Member;

public record FamilyMemberSummary(
	Long memberId,
	String nickname,
	String email,
	boolean familyOwner,
	LocalDateTime joinedAt
) {

	public static FamilyMemberSummary from(Member member) {
		return new FamilyMemberSummary(
			member.getId(),
			member.getNickname(),
			member.getEmail(),
			member.isFamilyOwner(),
			member.getCreatedAt()
		);
	}

}
