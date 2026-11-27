package com.petCare.petCare.domain.medicine.entity;

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
@Table(name = "medicine_catalog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicineCatalog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "external_code", nullable = false, unique = true, length = 100)
	private String externalCode;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "manufacturer", length = 100)
	private String manufacturer;

	@Column(name = "efficacy", columnDefinition = "TEXT")
	private String efficacy;

	@Column(name = "dosage_guide", columnDefinition = "TEXT")
	private String dosageGuide;

	@Column(name = "precautions", columnDefinition = "TEXT")
	private String precautions;

	@Builder
	public MedicineCatalog(
		String externalCode,
		String name,
		String manufacturer,
		String efficacy,
		String dosageGuide,
		String precautions
	) {
		this.externalCode = externalCode;
		this.name = name;
		this.manufacturer = manufacturer;
		this.efficacy = efficacy;
		this.dosageGuide = dosageGuide;
		this.precautions = precautions;
	}

}
