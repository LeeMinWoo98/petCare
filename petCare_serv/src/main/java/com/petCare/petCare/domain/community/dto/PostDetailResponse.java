package com.petCare.petCare.domain.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.petCare.petCare.domain.community.entity.Post;
import com.petCare.petCare.domain.community.entity.PostCategory;
import com.petCare.petCare.domain.member.entity.Member;

public record PostDetailResponse(
	Long id,
	PostCategory category,
	String title,
	String content,
	Long authorId,
	String authorNickname,
	long viewCount,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<CommentResponse> comments
) {
	public static PostDetailResponse of(Post post, long pendingDelta, List<CommentResponse> comments) {
		Member author = post.getAuthor();
		String nickname = (author != null && author.getDeletedAt() == null)
			? author.getNickname()
			: "탈퇴한 회원";

		return new PostDetailResponse(
			post.getId(),
			post.getCategory(),
			post.getTitle(),
			post.getContent(),
			author != null ? author.getId() : null,
			nickname,
			post.getViewCount() + pendingDelta,
			post.getCreatedAt(),
			post.getUpdatedAt(),
			comments
		);
	}
}
