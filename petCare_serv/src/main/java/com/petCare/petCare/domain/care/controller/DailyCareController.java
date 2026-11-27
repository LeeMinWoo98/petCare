package com.petCare.petCare.domain.care.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petCare.petCare.domain.care.dto.CareLogCreateRequest;
import com.petCare.petCare.domain.care.dto.CareLogItemResponse;
import com.petCare.petCare.domain.care.dto.DailyCareSummaryResponse;
import com.petCare.petCare.domain.care.service.DailyCareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Daily Care", description = "당일 산책·식사·간식 공유 기록 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DailyCareController {

	private final DailyCareService dailyCareService;

	@Operation(summary = "특정 날짜 케어 기록 조회", description = "반려동물의 산책/식사/간식 횟수 요약과 수행 이력을 조회합니다. 날짜 생략 시 오늘(KST) 기준입니다.")
	@GetMapping("/pets/{petId}/care-logs")
	public ResponseEntity<DailyCareSummaryResponse> getDailyCareLogs(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return ResponseEntity.ok(dailyCareService.getDailyCareSummary(memberId, petId, date));
	}

	@Operation(summary = "케어 기록 추가", description = "산책(WALK), 식사(MEAL), 간식(SNACK) 기록을 추가하고 가족에게 실시간 이벤트를 전파합니다.")
	@PostMapping("/pets/{petId}/care-logs")
	public ResponseEntity<CareLogItemResponse> createCareLog(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long petId,
		@Valid @RequestBody CareLogCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(dailyCareService.addCareLog(memberId, petId, request));
	}

	@Operation(summary = "케어 기록 취소(삭제)", description = "잘못 등록한 케어 기록을 삭제하고 가족에게 실시간 이벤트를 전파합니다.")
	@DeleteMapping("/care-logs/{logId}")
	public ResponseEntity<Void> deleteCareLog(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long logId
	) {
		dailyCareService.deleteCareLog(memberId, logId);
		return ResponseEntity.noContent().build();
	}

}
