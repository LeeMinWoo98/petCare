package com.petCare.petCare.domain.community.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.community.dto.CommentCreateRequest;
import com.petCare.petCare.domain.community.dto.CommentResponse;
import com.petCare.petCare.domain.community.dto.PostDetailResponse;
import com.petCare.petCare.domain.community.dto.PostPageResponse;
import com.petCare.petCare.domain.community.dto.PostSummaryResponse;
import com.petCare.petCare.domain.community.dto.PostUpsertRequest;
import com.petCare.petCare.domain.community.entity.Comment;
import com.petCare.petCare.domain.community.entity.Post;
import com.petCare.petCare.domain.community.entity.PostCategory;
import com.petCare.petCare.domain.community.repository.CommentRepository;
import com.petCare.petCare.domain.community.repository.PostRepository;
import com.petCare.petCare.domain.community.view.PostViewTracker;
import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.member.entity.MemberRole;
import com.petCare.petCare.global.auth.CurrentMemberService;
import com.petCare.petCare.global.exception.BusinessException;
import com.petCare.petCare.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

	private static final int MAX_PAGE_SIZE = 50;

	private final CurrentMemberService currentMemberService;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final PostViewTracker viewTracker;

	public PostPageResponse listPosts(PostCategory category, String keyword, int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
		String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

		Page<Post> postPage = postRepository.searchActivePosts(
			category,
			normalizedKeyword,
			PageRequest.of(safePage, safeSize)
		);

		List<Post> posts = postPage.getContent();
		if (posts.isEmpty()) {
			return new PostPageResponse(
				List.of(),
				postPage.getNumber(),
				postPage.getSize(),
				postPage.getTotalElements(),
				postPage.getTotalPages(),
				postPage.hasNext()
			);
		}

		List<Long> postIds = posts.stream().map(Post::getId).toList();
		Map<Long, Long> deltaMap = viewTracker.getPendingDeltas(postIds);
		Map<Long, Long> commentCountMap = loadCommentCounts(postIds);

		List<PostSummaryResponse> summaries = posts.stream()
			.map(post -> PostSummaryResponse.of(
				post,
				deltaMap.getOrDefault(post.getId(), 0L),
				commentCountMap.getOrDefault(post.getId(), 0L)
			))
			.toList();

		return new PostPageResponse(
			summaries,
			postPage.getNumber(),
			postPage.getSize(),
			postPage.getTotalElements(),
			postPage.getTotalPages(),
			postPage.hasNext()
		);
	}

	public PostDetailResponse getPostDetail(Long postId, String viewerKey) {
		Post post = postRepository.findActiveByIdWithAuthor(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

		long pendingDelta = viewTracker.recordViewAndGetDelta(post.getId(), viewerKey);
		List<CommentResponse> comments = commentRepository.findAllWithAuthorByPostId(post.getId())
			.stream()
			.map(CommentResponse::from)
			.toList();

		return PostDetailResponse.of(post, pendingDelta, comments);
	}

	@Transactional
	public PostDetailResponse createPost(Long memberId, PostUpsertRequest request) {
		Member author = currentMemberService.requireActiveMember(memberId);
		Post saved = postRepository.save(Post.builder()
			.author(author)
			.category(request.category())
			.title(request.title().trim())
			.content(request.content().trim())
			.build());
		return PostDetailResponse.of(saved, 0L, List.of());
	}

	@Transactional
	public PostDetailResponse updatePost(Long memberId, Long postId, PostUpsertRequest request) {
		Member actor = currentMemberService.requireActiveMember(memberId);
		Post post = postRepository.findActiveByIdWithAuthor(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		requireAuthorOrAdmin(actor, post.getAuthor().getId());

		post.update(request.category(), request.title().trim(), request.content().trim());

		long pendingDelta = viewTracker.getPendingDelta(post.getId());
		List<CommentResponse> comments = commentRepository.findAllWithAuthorByPostId(post.getId())
			.stream()
			.map(CommentResponse::from)
			.toList();
		return PostDetailResponse.of(post, pendingDelta, comments);
	}

	@Transactional
	public void deletePost(Long memberId, Long postId) {
		Member actor = currentMemberService.requireActiveMember(memberId);
		Post post = postRepository.findActiveByIdWithAuthor(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		requireAuthorOrAdmin(actor, post.getAuthor().getId());
		post.softDelete();
	}

	@Transactional
	public CommentResponse addComment(Long memberId, Long postId, CommentCreateRequest request) {
		Member author = currentMemberService.requireActiveMember(memberId);
		Post post = postRepository.findActiveByIdWithAuthor(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

		Comment saved = commentRepository.save(Comment.builder()
			.post(post)
			.author(author)
			.content(request.content().trim())
			.build());
		return CommentResponse.from(saved);
	}

	@Transactional
	public void deleteComment(Long memberId, Long postId, Long commentId) {
		Member actor = currentMemberService.requireActiveMember(memberId);
		Comment comment = commentRepository.findByIdAndPostIdWithAuthor(commentId, postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
		if (comment.getPost().getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.POST_NOT_FOUND);
		}
		requireAuthorOrAdmin(actor, comment.getAuthor().getId());
		commentRepository.delete(comment);
	}

	@Transactional
	public int flushBufferedViewsToDb() {
		Map<Long, Long> deltas = viewTracker.drainAllDeltas();
		int updatedPosts = 0;
		for (Map.Entry<Long, Long> entry : deltas.entrySet()) {
			if (entry.getValue() > 0) {
				updatedPosts += postRepository.addViewCount(entry.getKey(), entry.getValue());
			}
		}
		return updatedPosts;
	}

	private void requireAuthorOrAdmin(Member actor, Long ownerId) {
		boolean isOwner = actor.getId().equals(ownerId);
		boolean isAdmin = actor.getRole() == MemberRole.ADMIN;
		if (!isOwner && !isAdmin) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	private Map<Long, Long> loadCommentCounts(List<Long> postIds) {
		if (postIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : commentRepository.countByPostIds(postIds)) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return counts;
	}

}
