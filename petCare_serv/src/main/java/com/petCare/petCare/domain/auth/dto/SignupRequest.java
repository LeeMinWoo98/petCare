package com.petCare.petCare.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100)
	String email,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 4, max = 100, message = "비밀번호는 4자 이상 입력해 주세요.")
	String password,

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요.")
	String nickname,

	@NotNull(message = "가입 방식을 선택해 주세요.")
	SignupMode mode,

	@Size(max = 50, message = "가족 별칭은 50자 이하로 입력해 주세요.")
	String familyName,

	@Size(max = 20)
	String inviteCode
) {
}
