package com.petCare.petCare.domain.family.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.family.dto.FamilyMemberSummary;
import com.petCare.petCare.domain.family.dto.FamilyResponse;
import com.petCare.petCare.domain.family.entity.Family;
import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.member.repository.MemberRepository;
import com.petCare.petCare.global.auth.CurrentMemberService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyService {

	private final CurrentMemberService currentMemberService;
	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public FamilyResponse getMyFamily(Long memberId) {
		Member me = currentMemberService.requireMemberWithFamily(memberId);
		Family family = me.getFamily();
		List<FamilyMemberSummary> members = memberRepository
			.findAllByFamilyIdAndDeletedAtIsNullOrderByIdAsc(family.getId())
			.stream()
			.map(FamilyMemberSummary::from)
			.toList();
		return FamilyResponse.of(family, members);
	}

}
