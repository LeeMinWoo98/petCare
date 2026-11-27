package com.petCare.petCare.domain.medicine.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.medicine.dto.MedicineCatalogItemResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMedicineSearchCacheStore implements MedicineSearchCacheStore {

	private static final Duration CACHE_TTL = Duration.ofHours(1);

	private final StringRedisTemplate stringRedisTemplate;
	private final JsonMapper jsonMapper;

	@Override
	public Optional<List<MedicineCatalogItemResponse>> get(String keyword) {
		String key = MedicineSearchCacheStore.buildKey(keyword);
		try {
			String cached = stringRedisTemplate.opsForValue().get(key);
			if (cached == null) {
				return Optional.empty();
			}
			MedicineCatalogItemResponse[] items = jsonMapper.readValue(cached, MedicineCatalogItemResponse[].class);
			return Optional.of(Arrays.asList(items));
		} catch (Exception ex) {
			log.warn("Redis medicine search cache read failed for key={}", key, ex);
			return Optional.empty();
		}
	}

	@Override
	public void put(String keyword, List<MedicineCatalogItemResponse> items) {
		String key = MedicineSearchCacheStore.buildKey(keyword);
		try {
			String serialized = jsonMapper.writeValueAsString(items);
			stringRedisTemplate.opsForValue().set(key, serialized, CACHE_TTL);
		} catch (Exception ex) {
			log.warn("Redis medicine search cache write failed for key={}", key, ex);
		}
	}

	@Override
	public void clear() {
		try {
			Set<String> keys = stringRedisTemplate.keys("petcare:med:search:*");
			if (keys != null && !keys.isEmpty()) {
				stringRedisTemplate.delete(keys);
			}
		} catch (Exception ex) {
			log.warn("Redis medicine search cache clear failed", ex);
		}
	}

}
