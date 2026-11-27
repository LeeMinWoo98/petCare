package com.petCare.petCare.domain.family.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petCare.petCare.domain.family.entity.Family;

public interface FamilyRepository extends JpaRepository<Family, Long> {

	Optional<Family> findByInviteCode(String inviteCode);

	boolean existsByInviteCode(String inviteCode);

}
