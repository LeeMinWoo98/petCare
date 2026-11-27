package com.petCare.petCare.domain.pet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.pet.dto.PetProfileResponse;
import com.petCare.petCare.domain.pet.dto.PetUpsertRequest;
import com.petCare.petCare.domain.pet.entity.Pet;
import com.petCare.petCare.domain.pet.repository.PetRepository;
import com.petCare.petCare.global.auth.CurrentMemberService;
import com.petCare.petCare.global.exception.BusinessException;
import com.petCare.petCare.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyPetService {

	private final CurrentMemberService currentMemberService;
	private final PetRepository petRepository;

	public List<PetProfileResponse> listFamilyPets(Long memberId) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		return petRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByIdAsc(member.getFamily().getId())
			.stream()
			.map(PetProfileResponse::from)
			.toList();
	}

	public PetProfileResponse getFamilyPet(Long memberId, Long petId) {
		Pet pet = requireOwnedPet(memberId, petId);
		return PetProfileResponse.from(pet);
	}

	@Transactional
	public PetProfileResponse registerPet(Long memberId, PetUpsertRequest request) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Pet pet = Pet.builder()
			.family(member.getFamily())
			.name(request.name().trim())
			.species(request.species())
			.breed(trimToNull(request.breed()))
			.birthDate(request.birthDate())
			.gender(request.gender())
			.neutered(request.neutered())
			.weightKg(request.weightKg())
			.notes(trimToNull(request.notes()))
			.build();

		Pet saved = petRepository.save(pet);
		return PetProfileResponse.from(saved);
	}

	@Transactional
	public PetProfileResponse updateFamilyPet(Long memberId, Long petId, PetUpsertRequest request) {
		Pet pet = requireOwnedPet(memberId, petId);
		pet.update(
			request.name().trim(),
			request.species(),
			trimToNull(request.breed()),
			request.birthDate(),
			request.gender(),
			request.neutered(),
			request.weightKg(),
			trimToNull(request.notes())
		);
		return PetProfileResponse.from(pet);
	}

	@Transactional
	public void archiveFamilyPet(Long memberId, Long petId) {
		Pet pet = requireOwnedPet(memberId, petId);
		pet.softDelete();
	}

	private Pet requireOwnedPet(Long memberId, Long petId) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		return petRepository.findByIdAndFamilyIdAndDeletedAtIsNull(petId, member.getFamily().getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
