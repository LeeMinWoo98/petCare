package com.petCare.petCare.domain.pet.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "pets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "species", nullable = false, length = 20)
	private PetSpecies species;

	@Column(name = "breed", length = 50)
	private String breed;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", nullable = false, length = 20)
	private PetGender gender;

	@Column(name = "neutered", nullable = false)
	private boolean neutered;

	@Column(name = "weight_kg", precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Pet(
		Family family,
		String name,
		PetSpecies species,
		String breed,
		LocalDate birthDate,
		PetGender gender,
		boolean neutered,
		BigDecimal weightKg,
		String notes
	) {
		this.family = family;
		this.name = name;
		this.species = species;
		this.breed = breed;
		this.birthDate = birthDate;
		this.gender = gender != null ? gender : PetGender.UNKNOWN;
		this.neutered = neutered;
		this.weightKg = weightKg;
		this.notes = notes;
	}

	public void update(
		String name,
		PetSpecies species,
		String breed,
		LocalDate birthDate,
		PetGender gender,
		boolean neutered,
		BigDecimal weightKg,
		String notes
	) {
		this.name = name;
		this.species = species;
		this.breed = breed;
		this.birthDate = birthDate;
		this.gender = gender != null ? gender : PetGender.UNKNOWN;
		this.neutered = neutered;
		this.weightKg = weightKg;
		this.notes = notes;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

}
