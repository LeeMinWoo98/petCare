package com.petCare.petCare.domain.realtime.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.realtime.service.FamilySseRegistry;
import com.petCare.petCare.global.auth.CurrentMemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Tag(name = "Realtime", description = "가족 내 실시간 케어/투약 동기화 SSE API")
@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyEventController {

	private final CurrentMemberService currentMemberService;
	private final FamilySseRegistry sseRegistry;

	@Operation(
		summary = "가족 실시간 이벤트 구독 (SSE)",
		description = "EventSource 연결을 유지하며 가족 구성원의 케어 기록 추가/삭제 이벤트를 실시간 수신합니다."
	)
	@GetMapping(value = "/me/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribeFamilyEvents(
		@RequestHeader(value = "X-Member-Id", required = false) Long headerMemberId,
		@RequestParam(value = "memberId", required = false) Long queryMemberId,
		HttpServletResponse response
	) {
		Long resolvedMemberId = headerMemberId != null ? headerMemberId : queryMemberId;
		Member member = currentMemberService.requireMemberWithFamily(resolvedMemberId);
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("X-Accel-Buffering", "no");
		return sseRegistry.subscribe(member.getFamily().getId());
	}

}
