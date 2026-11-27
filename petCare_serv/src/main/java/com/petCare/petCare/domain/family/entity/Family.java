package com.petCare.petCare.domain.family.entity;

import com.petCare.petCare.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "families")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "invite_code", nullable = false, unique = true, length = 20)
	private String inviteCode;

	@Column(name = "name", length = 50)
	private String name;

	@Builder
	public Family(String inviteCode, String name) {
		this.inviteCode = inviteCode;
		this.name = name;
	}

}
