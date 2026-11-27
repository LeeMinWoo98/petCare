package com.petCare.petCare.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 가족 고유 번호입니다."),
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "가족 정보를 찾을 수 없습니다."),
	PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),
	CARE_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "케어 기록을 찾을 수 없습니다."),
	MEDICINE_CATALOG_NOT_FOUND(HttpStatus.NOT_FOUND, "의약품 카탈로그 정보를 찾을 수 없습니다."),
	PET_MEDICINE_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 약 정보를 찾을 수 없습니다."),
	MEDICATION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "투약 기록을 찾을 수 없습니다."),
	NO_REMAINING_DOSES(HttpStatus.BAD_REQUEST, "남은 투약 횟수가 없습니다. 약 수정에서 잔여량을 갱신해주세요."),
	CONCURRENT_MEDICATION_REQUEST(HttpStatus.CONFLICT, "다른 가족이 동시에 투약 체크 중입니다. 잠시 후 다시 확인해주세요."),
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;

}
