package com.petCare.petCare.domain.community.dto;

import java.time.LocalDateTime;

import com.petCare.petCare.domain.community.entity.Post;
import com.petCare.petCare.domain.community.entity.PostCategory;
import com.petCare.petCare.domain.member.entity.Member;

public record PostSummaryResponse(
	Long id,
	PostCategory category,
	String title,
	String excerpt,
	Long authorId,
	String authorNickname,
	long viewCount,
	long commentCount,
	LocalDateTime createdAt
) {
	private static final int EXCERPT_LENGTH = 90;

	public static PostSummaryResponse of(Post post, long pendingDelta, long commentCount) {
		Member author = post.getAuthor();
		String nickname = (author != null && author.getDeletedAt() == null)
			? author.getNickname()
			: "탈퇴한 회원";

		String normalized = post.getContent().replaceAll("\\s+", " ").trim();
		String excerpt = normalized.length() > EXCERPT_LENGTH
			? normalized.substring(0, EXCERPT_LENGTH) + "..."
			: normalized;

		return new PostSummaryResponse(
			post.getId(),
			post.getCategory(),
			post.getTitle(),
			excerpt,
			author != null ? author.getId() : null,
			nickname,
			post.getViewCount() + pendingDelta,
			commentCount,
			post.getCreatedAt()
		);
	}
}
