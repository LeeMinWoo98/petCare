package com.petCare.petCare.domain.pet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petCare.petCare.domain.pet.dto.PetProfileResponse;
import com.petCare.petCare.domain.pet.dto.PetUpsertRequest;
import com.petCare.petCare.domain.pet.service.FamilyPetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pet", description = "가족 공유 반려동물 프로필 CRUD API")
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class FamilyPetController {

	private final FamilyPetService familyPetService;

	@Operation(summary = "우리 가족 반려동물 목록 조회", description = "로그인 사용자의 가족에 등록된 활성 반려동물 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<List<PetProfileResponse>> listPets(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId
	) {
		return ResponseEntity.ok(familyPetService.listFamilyPets(memberId));
	}

	@Operation(summary = "반려동물 상세 조회", description = "우리 가족에 소속된 특정 반려동물 프로필을 조회합니다.")
	@GetMapping("/{petId}")
	public ResponseEntity<PetProfileResponse> getPet(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId
	) {
		return ResponseEntity.ok(familyPetService.getFamilyPet(memberId, petId));
	}

	@Operation(summary = "반려동물 등록", description = "우리 가족 그룹에 새 반려동물 프로필을 등록합니다.")
	@PostMapping
	public ResponseEntity<PetProfileResponse> createPet(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@Valid @RequestBody PetUpsertRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(familyPetService.registerPet(memberId, request));
	}

	@Operation(summary = "반려동물 수정", description = "우리 가족 반려동물 프로필 정보를 수정합니다.")
	@PutMapping("/{petId}")
	public ResponseEntity<PetProfileResponse> updatePet(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@Valid @RequestBody PetUpsertRequest request
	) {
		return ResponseEntity.ok(familyPetService.updateFamilyPet(memberId, petId, request));
	}

	@Operation(summary = "반려동물 삭제", description = "우리 가족 반려동물 프로필을 소프트 삭제합니다.")
	@DeleteMapping("/{petId}")
	public ResponseEntity<Void> deletePet(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId
	) {
		familyPetService.archiveFamilyPet(memberId, petId);
		return ResponseEntity.noContent().build();
	}

}
