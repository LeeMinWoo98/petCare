package com.petCare.petCare.domain.medicine.lock;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryMedicationLockManager implements MedicationLockManager {

	private final Map<Long, String> locks = new ConcurrentHashMap<>();

	@Override
	public String tryLock(Long petMedicineId, Duration ttl) {
		String token = UUID.randomUUID().toString();
		String existing = locks.putIfAbsent(petMedicineId, token);
		return existing == null ? token : null;
	}

	@Override
	public void unlock(Long petMedicineId, String token) {
		if (token == null) {
			return;
		}
		locks.remove(petMedicineId, token);
	}

}
