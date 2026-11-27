package com.petCare.petCare.domain.auth.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.auth.dto.AuthResponse;
import com.petCare.petCare.domain.auth.dto.LoginRequest;
import com.petCare.petCare.domain.auth.dto.SignupMode;
import com.petCare.petCare.domain.auth.dto.SignupRequest;
import com.petCare.petCare.domain.family.entity.Family;
import com.petCare.petCare.domain.family.repository.FamilyRepository;
import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.member.entity.MemberRole;
import com.petCare.petCare.domain.member.repository.MemberRepository;
import com.petCare.petCare.global.exception.BusinessException;
import com.petCare.petCare.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final int CODE_LENGTH = 6;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final MemberRepository memberRepository;
	private final FamilyRepository familyRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public AuthResponse signup(SignupRequest req) {
		String normalizedEmail = req.email().trim().toLowerCase();
		if (memberRepository.existsByEmail(normalizedEmail)) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		Family family;
		boolean owner;

		if (req.mode() == SignupMode.CREATE_FAMILY) {
			String name = (req.familyName() == null || req.familyName().isBlank())
				? req.nickname().trim() + "네 가족"
				: req.familyName().trim();
			family = familyRepository.save(Family.builder()
				.inviteCode(nextUniqueInviteCode())
				.name(name)
				.build());
			owner = true;
		} else {
			String rawCode = req.inviteCode() == null ? "" : req.inviteCode().trim().toUpperCase();
			if (rawCode.isEmpty()) {
				throw new BusinessException(ErrorCode.INVALID_INVITE_CODE, "가족 고유 번호를 입력해 주세요.");
			}
			family = familyRepository.findByInviteCode(rawCode)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
			owner = false;
		}

		Member saved = memberRepository.save(Member.builder()
			.family(family)
			.email(normalizedEmail)
			.password(passwordEncoder.encode(req.password()))
			.nickname(req.nickname().trim())
			.role(MemberRole.USER)
			.familyOwner(owner)
			.build());

		return AuthResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest req) {
		String normalizedEmail = req.email().trim().toLowerCase();
		Member member = memberRepository.findActiveByEmailWithFamily(normalizedEmail)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(req.password(), member.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		return AuthResponse.from(member);
	}

	private String nextUniqueInviteCode() {
		for (int attempt = 0; attempt < 20; attempt++) {
			StringBuilder sb = new StringBuilder("PET-");
			for (int i = 0; i < CODE_LENGTH; i++) {
				sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
			}
			String candidate = sb.toString();
			if (!familyRepository.existsByInviteCode(candidate)) {
				return candidate;
			}
		}
		throw new BusinessException(ErrorCode.INTERNAL_ERROR, "가족 고유 번호 생성에 실패했습니다.");
	}

}
