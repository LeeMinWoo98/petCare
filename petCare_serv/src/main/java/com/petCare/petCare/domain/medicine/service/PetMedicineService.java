package com.petCare.petCare.domain.medicine.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.petCare.petCare.domain.medicine.dto.MedicationLogCreateRequest;
import com.petCare.petCare.domain.medicine.dto.MedicationLogItemResponse;
import com.petCare.petCare.domain.medicine.dto.PetMedicineCreateRequest;
import com.petCare.petCare.domain.medicine.dto.PetMedicineResponse;
import com.petCare.petCare.domain.medicine.dto.PetMedicineUpdateRequest;
import com.petCare.petCare.domain.medicine.entity.MedicationLog;
import com.petCare.petCare.domain.medicine.entity.MedicineCatalog;
import com.petCare.petCare.domain.medicine.entity.MedicineSourceType;
import com.petCare.petCare.domain.medicine.entity.PetMedicine;
import com.petCare.petCare.domain.medicine.lock.MedicationLockManager;
import com.petCare.petCare.domain.medicine.repository.MedicationLogRepository;
import com.petCare.petCare.domain.medicine.repository.MedicineCatalogRepository;
import com.petCare.petCare.domain.medicine.repository.PetMedicineRepository;
import com.petCare.petCare.domain.member.entity.Member;
import com.petCare.petCare.domain.pet.entity.Pet;
import com.petCare.petCare.domain.pet.repository.PetRepository;
import com.petCare.petCare.domain.realtime.dto.FamilyRealtimeEvent;
import com.petCare.petCare.domain.realtime.service.FamilyEventBroker;
import com.petCare.petCare.global.auth.CurrentMemberService;
import com.petCare.petCare.global.exception.BusinessException;
import com.petCare.petCare.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetMedicineService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final Duration LOCK_TTL = Duration.ofSeconds(5);

	private final CurrentMemberService currentMemberService;
	private final PetRepository petRepository;
	private final MedicineCatalogRepository catalogRepository;
	private final PetMedicineRepository petMedicineRepository;
	private final MedicationLogRepository medicationLogRepository;
	private final MedicationLockManager lockManager;
	private final TransactionTemplate transactionTemplate;
	private final FamilyEventBroker eventBroker;

	@Transactional(readOnly = true)
	public List<PetMedicineResponse> listPetMedicines(Long memberId, Long petId, LocalDate date) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Pet pet = requireOwnedPet(petId, member.getFamily().getId());
		LocalDate targetDate = date != null ? date : LocalDate.now(KST);

		List<PetMedicine> medicines = petMedicineRepository
			.findAllByPetIdAndDeletedAtIsNullOrderByActiveDescIdDesc(pet.getId());
		if (medicines.isEmpty()) {
			return List.of();
		}

		List<Long> medicineIds = medicines.stream().map(PetMedicine::getId).toList();
		Map<Long, List<MedicationLogItemResponse>> logsByMedicine = medicationLogRepository
			.findAllWithMemberByPetMedicineIdsAndDate(medicineIds, targetDate)
			.stream()
			.collect(Collectors.groupingBy(
				log -> log.getPetMedicine().getId(),
				Collectors.mapping(MedicationLogItemResponse::from, Collectors.toList())
			));

		return medicines.stream()
			.map(med -> PetMedicineResponse.of(med, logsByMedicine.getOrDefault(med.getId(), Collections.emptyList())))
			.toList();
	}

	@Transactional
	public PetMedicineResponse createPetMedicine(Long memberId, Long petId, PetMedicineCreateRequest request) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Pet pet = requireOwnedPet(petId, member.getFamily().getId());

		MedicineCatalog catalog = null;
		if (request.sourceType() == MedicineSourceType.API && request.catalogId() != null) {
			catalog = catalogRepository.findById(request.catalogId())
				.orElseThrow(() -> new BusinessException(ErrorCode.MEDICINE_CATALOG_NOT_FOUND));
		}

		int remaining = request.remainingDoses() != null ? request.remainingDoses() : request.totalDoses();
		LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now(KST);

		PetMedicine saved = petMedicineRepository.save(PetMedicine.builder()
			.pet(pet)
			.catalog(catalog)
			.createdBy(member)
			.sourceType(request.sourceType())
			.name(request.name().trim())
			.dosageMethod(request.dosageMethod().trim())
			.dailyDoseCount(request.dailyDoseCount())
			.totalDoses(request.totalDoses())
			.remainingDoses(remaining)
			.startDate(startDate)
			.active(true)
			.build());

		return PetMedicineResponse.of(saved, List.of());
	}

	@Transactional
	public PetMedicineResponse updatePetMedicine(
		Long memberId,
		Long petId,
		Long medicineId,
		PetMedicineUpdateRequest request
	) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Pet pet = requireOwnedPet(petId, member.getFamily().getId());
		PetMedicine medicine = petMedicineRepository.findByIdAndPetIdAndDeletedAtIsNull(medicineId, pet.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PET_MEDICINE_NOT_FOUND));

		LocalDate startDate = request.startDate() != null ? request.startDate() : medicine.getStartDate();
		medicine.update(
			request.name().trim(),
			request.dosageMethod().trim(),
			request.dailyDoseCount(),
			request.totalDoses(),
			request.remainingDoses(),
			startDate,
			request.active()
		);

		List<MedicationLogItemResponse> todayLogs = medicationLogRepository
			.findAllWithMemberByPetMedicineIdsAndDate(List.of(medicine.getId()), LocalDate.now(KST))
			.stream()
			.map(MedicationLogItemResponse::from)
			.toList();
		return PetMedicineResponse.of(medicine, todayLogs);
	}

	@Transactional
	public void deletePetMedicine(Long memberId, Long petId, Long medicineId) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Pet pet = requireOwnedPet(petId, member.getFamily().getId());
		PetMedicine medicine = petMedicineRepository.findByIdAndPetIdAndDeletedAtIsNull(medicineId, pet.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PET_MEDICINE_NOT_FOUND));
		medicine.softDelete();
	}

	public MedicationLogItemResponse recordMedicationDose(
		Long memberId,
		Long petId,
		Long medicineId,
		MedicationLogCreateRequest request
	) {
		String lockToken = lockManager.tryLock(medicineId, LOCK_TTL);
		if (lockToken == null) {
			throw new BusinessException(ErrorCode.CONCURRENT_MEDICATION_REQUEST);
		}

		try {
			return transactionTemplate.execute(status -> {
				Member member = currentMemberService.requireMemberWithFamily(memberId);
				Long familyId = member.getFamily().getId();
				Pet pet = requireOwnedPet(petId, familyId);
				PetMedicine medicine = petMedicineRepository.findByIdAndPetIdAndDeletedAtIsNull(medicineId, pet.getId())
					.orElseThrow(() -> new BusinessException(ErrorCode.PET_MEDICINE_NOT_FOUND));

				medicine.consumeOneDose();

				LocalDateTime medicatedAt = (request != null && request.medicatedAt() != null)
					? request.medicatedAt()
					: LocalDateTime.now(KST);
				LocalDate medicatedDate = medicatedAt.toLocalDate();
				String memo = request != null ? trimToNull(request.memo()) : null;

				MedicationLog saved = medicationLogRepository.save(MedicationLog.builder()
					.petMedicine(medicine)
					.member(member)
					.medicatedDate(medicatedDate)
					.medicatedAt(medicatedAt)
					.memo(memo)
					.build());

				publishAfterCommit(FamilyRealtimeEvent.medicationLogChanged(
					"MEDICATION_LOG_CREATED",
					familyId,
					pet.getId(),
					medicatedDate,
					member.getId(),
					member.getNickname(),
					medicine.getId(),
					medicine.getName(),
					saved.getId()
				));

				return MedicationLogItemResponse.from(saved);
			});
		} finally {
			lockManager.unlock(medicineId, lockToken);
		}
	}

	public void cancelMedicationLog(Long memberId, Long logId) {
		Long medicineId = transactionTemplate.execute(status -> {
			Member member = currentMemberService.requireMemberWithFamily(memberId);
			MedicationLog log = medicationLogRepository.findByIdWithMedicineAndFamily(logId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_LOG_NOT_FOUND));
			Long petFamilyId = log.getPetMedicine().getPet().getFamily().getId();
			if (!member.getFamily().getId().equals(petFamilyId)
				|| log.getPetMedicine().getPet().getDeletedAt() != null
				|| log.getPetMedicine().getDeletedAt() != null) {
				throw new BusinessException(ErrorCode.MEDICATION_LOG_NOT_FOUND);
			}
			return log.getPetMedicine().getId();
		});

		String lockToken = lockManager.tryLock(medicineId, LOCK_TTL);
		if (lockToken == null) {
			throw new BusinessException(ErrorCode.CONCURRENT_MEDICATION_REQUEST);
		}

		try {
			transactionTemplate.executeWithoutResult(status -> {
				Member member = currentMemberService.requireMemberWithFamily(memberId);
				Long familyId = member.getFamily().getId();
				MedicationLog log = medicationLogRepository.findByIdWithMedicineAndFamily(logId)
					.orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_LOG_NOT_FOUND));

				PetMedicine medicine = log.getPetMedicine();
				if (!familyId.equals(medicine.getPet().getFamily().getId())) {
					throw new BusinessException(ErrorCode.MEDICATION_LOG_NOT_FOUND);
				}

				Long petId = medicine.getPet().getId();
				LocalDate medicatedDate = log.getMedicatedDate();
				String medicineName = medicine.getName();

				medicationLogRepository.delete(log);
				medicine.restoreOneDose();

				publishAfterCommit(FamilyRealtimeEvent.medicationLogChanged(
					"MEDICATION_LOG_DELETED",
					familyId,
					petId,
					medicatedDate,
					member.getId(),
					member.getNickname(),
					medicine.getId(),
					medicineName,
					logId
				));
			});
		} finally {
			lockManager.unlock(medicineId, lockToken);
		}
	}

	private Pet requireOwnedPet(Long petId, Long familyId) {
		return petRepository.findByIdAndFamilyIdAndDeletedAtIsNull(petId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
	}

	private void publishAfterCommit(FamilyRealtimeEvent event) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					eventBroker.publish(event);
				}
			});
		} else {
			eventBroker.publish(event);
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
