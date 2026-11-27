package com.petCare.petCare.domain.medicine.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petCare.petCare.domain.medicine.dto.MedicationLogCreateRequest;
import com.petCare.petCare.domain.medicine.dto.MedicationLogItemResponse;
import com.petCare.petCare.domain.medicine.dto.MedicineCatalogItemResponse;
import com.petCare.petCare.domain.medicine.dto.PetMedicineCreateRequest;
import com.petCare.petCare.domain.medicine.dto.PetMedicineResponse;
import com.petCare.petCare.domain.medicine.dto.PetMedicineUpdateRequest;
import com.petCare.petCare.domain.medicine.service.MedicineCatalogService;
import com.petCare.petCare.domain.medicine.service.PetMedicineService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Medicine", description = "동물 의약품 카탈로그 검색, 복용 약 관리, 투약 체크 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MedicineController {

	private final MedicineCatalogService catalogService;
	private final PetMedicineService petMedicineService;

	@Operation(summary = "동물 의약품 카탈로그 검색", description = "의약품 이름 또는 제조사로 등록된 동물용 의약품 카탈로그를 검색합니다.")
	@GetMapping("/medicines/search")
	public ResponseEntity<List<MedicineCatalogItemResponse>> searchCatalog(
		@RequestParam(value = "keyword", required = false) String keyword
	) {
		return ResponseEntity.ok(catalogService.search(keyword));
	}

	@Operation(summary = "반려동물 복용 약 목록 조회", description = "반려동물의 약 목록과 오늘 투약 현황, 잔여 일수를 조회합니다.")
	@GetMapping("/pets/{petId}/medicines")
	public ResponseEntity<List<PetMedicineResponse>> listMedicines(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return ResponseEntity.ok(petMedicineService.listPetMedicines(memberId, petId, date));
	}

	@Operation(summary = "반려동물 약 등록", description = "카탈로그 검색(API) 또는 직접 입력(CUSTOM)으로 복용 약과 급여 방법을 등록합니다.")
	@PostMapping("/pets/{petId}/medicines")
	public ResponseEntity<PetMedicineResponse> createMedicine(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@Valid @RequestBody PetMedicineCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(petMedicineService.createPetMedicine(memberId, petId, request));
	}

	@Operation(summary = "반려동물 약 정보 수정", description = "급여 방법, 1일 횟수, 잔여량, 복용 중 상태를 수정합니다.")
	@PutMapping("/pets/{petId}/medicines/{medicineId}")
	public ResponseEntity<PetMedicineResponse> updateMedicine(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@PathVariable Long medicineId,
		@Valid @RequestBody PetMedicineUpdateRequest request
	) {
		return ResponseEntity.ok(petMedicineService.updatePetMedicine(memberId, petId, medicineId, request));
	}

	@Operation(summary = "반려동물 약 삭제", description = "등록된 약을 소프트 삭제합니다.")
	@DeleteMapping("/pets/{petId}/medicines/{medicineId}")
	public ResponseEntity<Void> deleteMedicine(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@PathVariable Long medicineId
	) {
		petMedicineService.deletePetMedicine(memberId, petId, medicineId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "오늘 투약 완료 체크", description = "투약 기록을 남기고 잔여 투약 횟수를 1 차감합니다. 동시 클릭 방지 락이 적용됩니다.")
	@PostMapping("/pets/{petId}/medicines/{medicineId}/logs")
	public ResponseEntity<MedicationLogItemResponse> recordMedicationDose(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@PathVariable Long medicineId,
		@Valid @RequestBody(required = false) MedicationLogCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(petMedicineService.recordMedicationDose(memberId, petId, medicineId, request));
	}

	@Operation(summary = "투약 기록 취소", description = "잘못 누른 투약 기록을 삭제하고 잔여 투약 횟수를 1 복구합니다.")
	@DeleteMapping("/medication-logs/{logId}")
	public ResponseEntity<Void> cancelMedicationLog(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long logId
	) {
		petMedicineService.cancelMedicationLog(memberId, logId);
		return ResponseEntity.noContent().build();
	}

}
