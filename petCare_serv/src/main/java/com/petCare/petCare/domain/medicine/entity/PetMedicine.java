package com.petCare.petCare.domain.medicine.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.pet.entity.Pet;
import com.petCare.petCare.global.entity.BaseTimeEntity;
import com.petCare.petCare.global.exception.BusinessException;
import com.petCare.petCare.global.exception.ErrorCode;

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
@Table(name = "pet_medicines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetMedicine extends BaseTimeEntity {

	private static final int LOW_STOCK_DAYS_THRESHOLD = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pet_id", nullable = false)
	private Pet pet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "catalog_id")
	private MedicineCatalog catalog;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private Member createdBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 20)
	private MedicineSourceType sourceType;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "dosage_method", nullable = false, columnDefinition = "TEXT")
	private String dosageMethod;

	@Column(name = "daily_dose_count", nullable = false)
	private int dailyDoseCount;

	@Column(name = "total_doses", nullable = false)
	private int totalDoses;

	@Column(name = "remaining_doses", nullable = false)
	private int remainingDoses;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public PetMedicine(
		Pet pet,
		MedicineCatalog catalog,
		Member createdBy,
		MedicineSourceType sourceType,
		String name,
		String dosageMethod,
		int dailyDoseCount,
		int totalDoses,
		int remainingDoses,
		LocalDate startDate,
		boolean active
	) {
		this.pet = pet;
		this.catalog = catalog;
		this.createdBy = createdBy;
		this.sourceType = sourceType;
		this.name = name;
		this.dosageMethod = dosageMethod;
		this.dailyDoseCount = Math.max(1, dailyDoseCount);
		this.totalDoses = Math.max(1, totalDoses);
		this.remainingDoses = Math.max(0, remainingDoses);
		this.startDate = startDate;
		this.active = active;
	}

	public void update(
		String name,
		String dosageMethod,
		int dailyDoseCount,
		int totalDoses,
		int remainingDoses,
		LocalDate startDate,
		boolean active
	) {
		this.name = name;
		this.dosageMethod = dosageMethod;
		this.dailyDoseCount = Math.max(1, dailyDoseCount);
		this.totalDoses = Math.max(1, totalDoses);
		this.remainingDoses = Math.max(0, remainingDoses);
		this.startDate = startDate;
		this.active = active;
	}

	public void consumeOneDose() {
		if (this.remainingDoses <= 0) {
			throw new BusinessException(ErrorCode.NO_REMAINING_DOSES);
		}
		this.remainingDoses -= 1;
	}

	public void restoreOneDose() {
		if (this.remainingDoses < this.totalDoses) {
			this.remainingDoses += 1;
		}
	}

	public int calculateRemainingDays() {
		if (this.remainingDoses <= 0) {
			return 0;
		}
		return (int) Math.ceil((double) this.remainingDoses / this.dailyDoseCount);
	}

	public boolean isLowStock() {
		int days = calculateRemainingDays();
		return this.active && this.remainingDoses > 0 && days <= LOW_STOCK_DAYS_THRESHOLD;
	}

	public boolean isDepleted() {
		return this.remainingDoses <= 0;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
		this.active = false;
	}

}
