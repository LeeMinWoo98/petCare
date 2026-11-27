package com.petCare.petCare.domain.medicine.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petCare.petCare.domain.medicine.entity.MedicationLog;

public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {

	@Query("""
		select ml from MedicationLog ml
		join fetch ml.member
		where ml.petMedicine.id in :medicineIds
		  and ml.medicatedDate = :medicatedDate
		order by ml.medicatedAt desc, ml.id desc
		""")
	List<MedicationLog> findAllWithMemberByPetMedicineIdsAndDate(
		@Param("medicineIds") List<Long> medicineIds,
		@Param("medicatedDate") LocalDate medicatedDate
	);

	@Query("""
		select ml from MedicationLog ml
		join fetch ml.petMedicine pm
		join fetch pm.pet p
		join fetch p.family
		where ml.id = :logId
		""")
	Optional<MedicationLog> findByIdWithMedicineAndFamily(@Param("logId") Long logId);

}
