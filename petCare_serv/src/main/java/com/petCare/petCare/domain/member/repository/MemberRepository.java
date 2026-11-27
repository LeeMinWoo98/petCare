package com.petCare.petCare.domain.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petCare.petCare.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);

	@Query("select m from Member m left join fetch m.family where m.email = :email and m.deletedAt is null")
	Optional<Member> findActiveByEmailWithFamily(@Param("email") String email);

	@Query("select m from Member m left join fetch m.family where m.id = :id and m.deletedAt is null")
	Optional<Member> findActiveByIdWithFamily(@Param("id") Long id);

	List<Member> findAllByFamilyIdAndDeletedAtIsNullOrderByIdAsc(Long familyId);

}
