package com.petCare.petCare.domain.community.dto;

import com.petCare.petCare.domain.community.entity.PostCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostUpsertRequest(
	@NotNull(message = "카테고리를 선택해주세요.")
	PostCategory category,

	@NotBlank(message = "제목을 입력해주세요.")
	@Size(max = 150, message = "제목은 150자 이하로 입력해주세요.")
	String title,

	@NotBlank(message = "본문 내용을 입력해주세요.")
	@Size(max = 10000, message = "본문은 10,000자 이하로 입력해주세요.")
	String content
) {
}
