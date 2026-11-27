package com.petCare.petCare.domain.care.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.pet.entity.Pet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "daily_care_logs")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCareLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pet_id", nullable = false)
	private Pet pet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "care_date", nullable = false)
	private LocalDate careDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "care_type", nullable = false, length = 20)
	private CareType careType;

	@Column(name = "performed_at", nullable = false)
	private LocalDateTime performedAt;

	@Column(name = "memo", length = 200)
	private String memo;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public DailyCareLog(
		Pet pet,
		Member member,
		LocalDate careDate,
		CareType careType,
		LocalDateTime performedAt,
		String memo
	) {
		this.pet = pet;
		this.member = member;
		this.careDate = careDate;
		this.careType = careType;
		this.performedAt = performedAt;
		this.memo = memo;
	}

}
