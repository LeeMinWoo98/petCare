package com.petCare.petCare.domain.care.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.petCare.petCare.domain.care.cache.DailyCareCacheStore;
import com.petCare.petCare.domain.care.dto.CareLogCreateRequest;
import com.petCare.petCare.domain.care.dto.CareLogItemResponse;
import com.petCare.petCare.domain.care.dto.DailyCareSummaryResponse;
import com.petCare.petCare.domain.care.entity.CareType;
import com.petCare.petCare.domain.care.entity.DailyCareLog;
import com.petCare.petCare.domain.care.repository.DailyCareLogRepository;
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
@Transactional(readOnly = true)
public class DailyCareService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final CurrentMemberService currentMemberService;
	private final PetRepository petRepository;
	private final DailyCareLogRepository careLogRepository;
	private final DailyCareCacheStore cacheStore;
	private final FamilyEventBroker eventBroker;

	public DailyCareSummaryResponse getDailyCareSummary(Long memberId, Long petId, LocalDate date) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Long familyId = member.getFamily().getId();
		Pet pet = requireOwnedPet(petId, familyId);
		LocalDate targetDate = date != null ? date : LocalDate.now(KST);

		return cacheStore.get(familyId, pet.getId(), targetDate)
			.orElseGet(() -> {
				List<CareLogItemResponse> items = careLogRepository
					.findAllWithMemberByPetIdAndCareDate(pet.getId(), targetDate)
					.stream()
					.map(CareLogItemResponse::from)
					.toList();
				DailyCareSummaryResponse summary = DailyCareSummaryResponse.of(pet.getId(), targetDate, items);
				cacheStore.put(familyId, pet.getId(), targetDate, summary);
				return summary;
			});
	}

	@Transactional
	public CareLogItemResponse addCareLog(Long memberId, Long petId, CareLogCreateRequest request) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Long familyId = member.getFamily().getId();
		Pet pet = requireOwnedPet(petId, familyId);

		LocalDateTime performedAt = request.performedAt() != null
			? request.performedAt()
			: LocalDateTime.now(KST);
		LocalDate careDate = performedAt.toLocalDate();

		DailyCareLog saved = careLogRepository.save(DailyCareLog.builder()
			.pet(pet)
			.member(member)
			.careDate(careDate)
			.careType(request.careType())
			.performedAt(performedAt)
			.memo(trimToNull(request.memo()))
			.build());

		cacheStore.evict(familyId, pet.getId(), careDate);
		publishAfterCommit(FamilyRealtimeEvent.careLogChanged(
			"CARE_LOG_CREATED",
			familyId,
			pet.getId(),
			careDate,
			member.getId(),
			member.getNickname(),
			saved.getCareType(),
			saved.getId()
		));

		return CareLogItemResponse.from(saved);
	}

	@Transactional
	public void deleteCareLog(Long memberId, Long logId) {
		Member member = currentMemberService.requireMemberWithFamily(memberId);
		Long familyId = member.getFamily().getId();

		DailyCareLog careLog = careLogRepository.findByIdWithPetAndFamily(logId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CARE_LOG_NOT_FOUND));

		Long petFamilyId = careLog.getPet().getFamily().getId();
		if (!familyId.equals(petFamilyId) || careLog.getPet().getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.CARE_LOG_NOT_FOUND);
		}

		Long petId = careLog.getPet().getId();
		LocalDate careDate = careLog.getCareDate();
		CareType careType = careLog.getCareType();

		careLogRepository.delete(careLog);
		cacheStore.evict(familyId, petId, careDate);

		publishAfterCommit(FamilyRealtimeEvent.careLogChanged(
			"CARE_LOG_DELETED",
			familyId,
			petId,
			careDate,
			member.getId(),
			member.getNickname(),
			careType,
			logId
		));
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
