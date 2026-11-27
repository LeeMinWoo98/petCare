package com.petCare.petCare.global.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.member.repository.MemberRepository;
import com.petCare.petCare.global.exception.BusinessException;
import com.petCare.petCare.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Step 6 JWT 도입 전까지 X-Member-Id 헤더로 현재 로그인 사용자를 조회하는 추상화 계층.
 * Step 6에서 SecurityContextHolder(JWT) 기반으로 이 클래스만 교체하면 모든 서비스가 그대로 동작합니다.
 */
@Service
@RequiredArgsConstructor
public class CurrentMemberService {

	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public Member requireActiveMember(Long memberId) {
		if (memberId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return memberRepository.findActiveByIdWithFamily(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public Member requireMemberWithFamily(Long memberId) {
		Member member = requireActiveMember(memberId);
		if (member.getFamily() == null) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
		}
		return member;
	}

}
