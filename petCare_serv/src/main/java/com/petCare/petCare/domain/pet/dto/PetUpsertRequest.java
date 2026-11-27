package com.petCare.petCare.domain.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.petCare.petCare.domain.pet.entity.PetGender;
import com.petCare.petCare.domain.pet.entity.PetSpecies;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PetUpsertRequest(
	@NotBlank(message = "반려동물 이름을 입력해주세요.")
	@Size(max = 50, message = "반려동물 이름은 50자 이하로 입력해주세요.")
	String name,

	@NotNull(message = "반려동물 종을 선택해주세요.")
	PetSpecies species,

	@Size(max = 50, message = "품종은 50자 이하로 입력해주세요.")
	String breed,

	LocalDate birthDate,

	PetGender gender,

	boolean neutered,

	@DecimalMin(value = "0.01", message = "몸무게는 0보다 커야 합니다.")
	@Digits(integer = 3, fraction = 2, message = "몸무게는 소수점 둘째 자리까지 입력할 수 있습니다.")
	BigDecimal weightKg,

	@Size(max = 2000, message = "특이사항은 2000자 이하로 입력해주세요.")
	String notes
) {
}
