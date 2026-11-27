package com.petCare.petCare.domain.family.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petCare.petCare.domain.family.dto.FamilyResponse;
import com.petCare.petCare.domain.family.service.FamilyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Family", description = "가족 그룹 및 구성원 조회 API")
@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {

	private final FamilyService familyService;

	@Operation(summary = "내 가족 정보 조회", description = "초대 코드 및 소속 가족 구성원 목록을 조회합니다.")
	@GetMapping("/me")
	public ResponseEntity<FamilyResponse> getMyFamily(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId
	) {
		return ResponseEntity.ok(familyService.getMyFamily(memberId));
	}

}
