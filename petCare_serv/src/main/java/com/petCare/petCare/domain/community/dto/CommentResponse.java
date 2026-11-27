package com.petCare.petCare.domain.community.dto;

import java.time.LocalDateTime;

import com.petCare.petCare.domain.community.entity.Comment;
import com.petCare.petCare.domain.member.entity.Member;

public record CommentResponse(
	Long id,
	Long postId,
	Long authorId,
	String authorNickname,
	String content,
	LocalDateTime createdAt
) {
	public static CommentResponse from(Comment comment) {
		Member author = comment.getAuthor();
		String nickname = (author != null && author.getDeletedAt() == null)
			? author.getNickname()
			: "탈퇴한 회원";

		return new CommentResponse(
			comment.getId(),
			comment.getPost().getId(),
			author != null ? author.getId() : null,
			nickname,
			comment.getContent(),
			comment.getCreatedAt()
		);
	}
}
