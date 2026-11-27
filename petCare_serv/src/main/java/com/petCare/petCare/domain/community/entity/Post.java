package com.petCare.petCare.domain.community.entity;

import java.time.LocalDateTime;

import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private Member author;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private PostCategory category;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Post(Member author, PostCategory category, String title, String content) {
		this.author = author;
		this.category = category;
		this.title = title;
		this.content = content;
		this.viewCount = 0L;
	}

	public void update(PostCategory category, String title, String content) {
		this.category = category;
		this.title = title;
		this.content = content;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

}
