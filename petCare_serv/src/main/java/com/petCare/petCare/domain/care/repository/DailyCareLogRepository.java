package com.petCare.petCare.domain.care.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petCare.petCare.domain.care.entity.DailyCareLog;

public interface DailyCareLogRepository extends JpaRepository<DailyCareLog, Long> {

	@Query("""
		select l from DailyCareLog l
		join fetch l.member
		where l.pet.id = :petId
		  and l.careDate = :careDate
		order by l.performedAt desc, l.id desc
		""")
	List<DailyCareLog> findAllWithMemberByPetIdAndCareDate(
		@Param("petId") Long petId,
		@Param("careDate") LocalDate careDate
	);

	@Query("""
		select l from DailyCareLog l
		join fetch l.pet p
		join fetch p.family
		where l.id = :logId
		""")
	Optional<DailyCareLog> findByIdWithPetAndFamily(@Param("logId") Long logId);

}
