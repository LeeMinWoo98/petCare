package com.petCare.petCare.domain.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.petCare.petCare.domain.pet.entity.Pet;
import com.petCare.petCare.domain.pet.entity.PetGender;
import com.petCare.petCare.domain.pet.entity.PetSpecies;

public record PetProfileResponse(
	Long id,
	Long familyId,
	String name,
	PetSpecies species,
	String breed,
	LocalDate birthDate,
	PetGender gender,
	boolean neutered,
	BigDecimal weightKg,
	String notes,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static PetProfileResponse from(Pet pet) {
		return new PetProfileResponse(
			pet.getId(),
			pet.getFamily().getId(),
			pet.getName(),
			pet.getSpecies(),
			pet.getBreed(),
			pet.getBirthDate(),
			pet.getGender(),
			pet.isNeutered(),
			pet.getWeightKg(),
			pet.getNotes(),
			pet.getCreatedAt(),
			pet.getUpdatedAt()
		);
	}
}
