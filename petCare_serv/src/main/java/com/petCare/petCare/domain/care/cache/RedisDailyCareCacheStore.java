package com.petCare.petCare.domain.care.cache;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.care.dto.DailyCareSummaryResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisDailyCareCacheStore implements DailyCareCacheStore {

	private static final Duration CACHE_TTL = Duration.ofMinutes(10);

	private final StringRedisTemplate stringRedisTemplate;
	private final JsonMapper jsonMapper;

	@Override
	public Optional<DailyCareSummaryResponse> get(Long familyId, Long petId, LocalDate date) {
		String key = DailyCareCacheStore.buildKey(familyId, petId, date);
		try {
			String cached = stringRedisTemplate.opsForValue().get(key);
			if (cached == null) {
				return Optional.empty();
			}
			return Optional.of(jsonMapper.readValue(cached, DailyCareSummaryResponse.class));
		} catch (Exception ex) {
			log.warn("Redis care cache read failed for key={}", key, ex);
			return Optional.empty();
		}
	}

	@Override
	public void put(Long familyId, Long petId, LocalDate date, DailyCareSummaryResponse summary) {
		String key = DailyCareCacheStore.buildKey(familyId, petId, date);
		try {
			String serialized = jsonMapper.writeValueAsString(summary);
			stringRedisTemplate.opsForValue().set(key, serialized, CACHE_TTL);
		} catch (Exception ex) {
			log.warn("Redis care cache write failed for key={}", key, ex);
		}
	}

	@Override
	public void evict(Long familyId, Long petId, LocalDate date) {
		String key = DailyCareCacheStore.buildKey(familyId, petId, date);
		try {
			stringRedisTemplate.delete(key);
		} catch (Exception ex) {
			log.warn("Redis care cache evict failed for key={}", key, ex);
		}
	}

}
