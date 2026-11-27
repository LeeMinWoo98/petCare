package com.petCare.petCare.domain.medicine.lock;

import java.time.Duration;

public interface MedicationLockManager {

	/**
	 * 락 획득 성공 시 고유 토큰 반환, 실패 시 null 반환.
	 */
	String tryLock(Long petMedicineId, Duration ttl);

	void unlock(Long petMedicineId, String token);

	static String buildLockKey(Long petMedicineId) {
		return "petcare:lock:med:" + petMedicineId;
	}

}
