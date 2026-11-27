package com.petCare.petCare.domain.medicine.lock;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMedicationLockManager implements MedicationLockManager {

	private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
		"if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
		Long.class
	);

	private final StringRedisTemplate stringRedisTemplate;
	private final Map<Long, String> localFallbackLocks = new ConcurrentHashMap<>();

	@Override
	public String tryLock(Long petMedicineId, Duration ttl) {
		String key = MedicationLockManager.buildLockKey(petMedicineId);
		String token = UUID.randomUUID().toString();
		try {
			Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl);
			return Boolean.TRUE.equals(acquired) ? token : null;
		} catch (Exception ex) {
			log.warn("Redis medication lock failed for key={}, falling back to local lock", key, ex);
			String existing = localFallbackLocks.putIfAbsent(petMedicineId, token);
			return existing == null ? token : null;
		}
	}

	@Override
	public void unlock(Long petMedicineId, String token) {
		if (token == null) {
			return;
		}
		localFallbackLocks.remove(petMedicineId, token);
		String key = MedicationLockManager.buildLockKey(petMedicineId);
		try {
			stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
		} catch (Exception ex) {
			log.warn("Redis medication unlock failed for key={}", key, ex);
		}
	}

}
