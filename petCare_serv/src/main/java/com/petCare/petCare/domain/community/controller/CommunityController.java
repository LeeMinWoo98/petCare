package com.petCare.petCare.domain.community.controller;

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

import com.petCare.petCare.domain.community.dto.CommentCreateRequest;
import com.petCare.petCare.domain.community.dto.CommentResponse;
import com.petCare.petCare.domain.community.dto.PostDetailResponse;
import com.petCare.petCare.domain.community.dto.PostPageResponse;
import com.petCare.petCare.domain.community.dto.PostUpsertRequest;
import com.petCare.petCare.domain.community.entity.PostCategory;
import com.petCare.petCare.domain.community.service.CommunityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Community", description = "반려동물 정보 공유 게시판 (글/댓글/검색/조회수) API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CommunityController {

	private final CommunityService communityService;

	@Operation(summary = "게시글 목록 조회", description = "카테고리/키워드 필터와 페이징으로 게시글 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<PostPageResponse> listPosts(
		@RequestParam(value = "category", required = false) PostCategory category,
		@RequestParam(value = "keyword", required = false) String keyword,
		@RequestParam(value = "page", defaultValue = "0") int page,
		@RequestParam(value = "size", defaultValue = "15") int size
	) {
		return ResponseEntity.ok(communityService.listPosts(category, keyword, page, size));
	}

	@Operation(summary = "게시글 작성", description = "새 커뮤니티 게시글을 작성합니다.")
	@PostMapping
	public ResponseEntity<PostDetailResponse> createPost(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@Valid @RequestBody PostUpsertRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(communityService.createPost(memberId, request));
	}

	@Operation(summary = "게시글 상세 조회", description = "게시글 상세와 댓글 목록을 조회하며, 24시간 중복 방지 조회수를 반영합니다.")
	@GetMapping("/{postId}")
	public ResponseEntity<PostDetailResponse> getPost(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long postId,
		HttpServletRequest servletRequest
	) {
		String viewerKey = resolveViewerKey(memberId, servletRequest);
		return ResponseEntity.ok(communityService.getPostDetail(postId, viewerKey));
	}

	@Operation(summary = "게시글 수정", description = "작성자 본인이 게시글을 수정합니다.")
	@PutMapping("/{postId}")
	public ResponseEntity<PostDetailResponse> updatePost(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long postId,
		@Valid @RequestBody PostUpsertRequest request
	) {
		return ResponseEntity.ok(communityService.updatePost(memberId, postId, request));
	}

	@Operation(summary = "게시글 삭제", description = "작성자 본인이 게시글을 소프트 삭제합니다.")
	@DeleteMapping("/{postId}")
	public ResponseEntity<Void> deletePost(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long postId
	) {
		communityService.deletePost(memberId, postId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
	@PostMapping("/{postId}/comments")
	public ResponseEntity<CommentResponse> addComment(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long postId,
		@Valid @RequestBody CommentCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(communityService.addComment(memberId, postId, request));
	}

	@Operation(summary = "댓글 삭제", description = "댓글 작성자 본인이 댓글을 삭제합니다.")
	@DeleteMapping("/{postId}/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(
		@RequestHeader(value = "X-Member-Id", required = false) Long memberId,
		@PathVariable Long postId,
		@PathVariable Long commentId
	) {
		communityService.deleteComment(memberId, postId, commentId);
		return ResponseEntity.noContent().build();
	}

	private String resolveViewerKey(Long memberId, HttpServletRequest request) {
		if (memberId != null) {
			return "member:" + memberId;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		String ip = (forwarded != null && !forwarded.isBlank())
			? forwarded.split(",")[0].trim()
			: request.getRemoteAddr();
		return "ip:" + (ip != null ? ip : "unknown");
	}

}
