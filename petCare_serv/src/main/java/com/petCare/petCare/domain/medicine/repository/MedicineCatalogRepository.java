package com.petCare.petCare.domain.medicine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petCare.petCare.domain.medicine.entity.MedicineCatalog;

public interface MedicineCatalogRepository extends JpaRepository<MedicineCatalog, Long> {

	List<MedicineCatalog> findTop20ByNameContainingIgnoreCaseOrManufacturerContainingIgnoreCaseOrderByNameAsc(
		String nameKeyword,
		String manufacturerKeyword
	);

	List<MedicineCatalog> findTop20ByOrderByNameAsc();

	boolean existsByExternalCode(String externalCode);

}
