package com.petCare.petCare.domain.medicine.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.petCare.petCare.domain.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "medication_logs")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pet_medicine_id", nullable = false)
	private PetMedicine petMedicine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "medicated_date", nullable = false)
	private LocalDate medicatedDate;

	@Column(name = "medicated_at", nullable = false)
	private LocalDateTime medicatedAt;

	@Column(name = "memo", length = 200)
	private String memo;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public MedicationLog(
		PetMedicine petMedicine,
		Member member,
		LocalDate medicatedDate,
		LocalDateTime medicatedAt,
		String memo
	) {
		this.petMedicine = petMedicine;
		this.member = member;
		this.medicatedDate = medicatedDate;
		this.medicatedAt = medicatedAt;
		this.memo = memo;
	}

}
