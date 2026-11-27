package com.petCare.petCare.domain.pet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petCare.petCare.domain.pet.entity.Pet;

public interface PetRepository extends JpaRepository<Pet, Long> {

	List<Pet> findAllByFamilyIdAndDeletedAtIsNullOrderByIdAsc(Long familyId);

	Optional<Pet> findByIdAndFamilyIdAndDeletedAtIsNull(Long id, Long familyId);

}
