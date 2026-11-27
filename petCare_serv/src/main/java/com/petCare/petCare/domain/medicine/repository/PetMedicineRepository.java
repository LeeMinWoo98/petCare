package com.petCare.petCare.domain.medicine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petCare.petCare.domain.medicine.entity.PetMedicine;

public interface PetMedicineRepository extends JpaRepository<PetMedicine, Long> {

	List<PetMedicine> findAllByPetIdAndDeletedAtIsNullOrderByActiveDescIdDesc(Long petId);

	Optional<PetMedicine> findByIdAndPetIdAndDeletedAtIsNull(Long id, Long petId);

	@Query("""
		select pm from PetMedicine pm
		join fetch pm.pet p
		join fetch p.family
		where pm.id = :medicineId
		  and pm.deletedAt is null
		""")
	Optional<PetMedicine> findActiveByIdWithPetAndFamily(@Param("medicineId") Long medicineId);

}
