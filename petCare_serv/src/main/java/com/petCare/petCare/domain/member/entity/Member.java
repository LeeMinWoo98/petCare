package com.petCare.petCare.domain.member.entity;

import java.time.LocalDateTime;

import com.petCare.petCare.domain.family.entity.Family;
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
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id")
	private Family family;

	@Column(name = "email", nullable = false, unique = true, length = 100)
	private String email;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "nickname", nullable = false, length = 30)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private MemberRole role;

	@Column(name = "is_family_owner", nullable = false)
	private boolean familyOwner;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Member(Family family, String email, String password, String nickname, MemberRole role, boolean familyOwner) {
		this.family = family;
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.role = role != null ? role : MemberRole.USER;
		this.familyOwner = familyOwner;
	}

}
