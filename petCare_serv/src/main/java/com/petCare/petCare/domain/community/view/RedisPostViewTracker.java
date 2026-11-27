package com.petCare.petCare.domain.community.view;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisPostViewTracker implements PostViewTracker {

	private static final String SEEN_PREFIX = "petcare:post:view:seen:";
	private static final String DELTA_HASH_KEY = "petcare:post:view:delta";
	private static final Duration SEEN_TTL = Duration.ofHours(24);

	private final StringRedisTemplate stringRedisTemplate;

	@Override
	public long recordViewAndGetDelta(Long postId, String viewerKey) {
		String seenKey = SEEN_PREFIX + postId + ":" + viewerKey;
		try {
			Boolean isFirstView = stringRedisTemplate.opsForValue().setIfAbsent(seenKey, "1", SEEN_TTL);
			if (Boolean.TRUE.equals(isFirstView)) {
				Long updated = stringRedisTemplate.opsForHash().increment(DELTA_HASH_KEY, String.valueOf(postId), 1L);
				return updated != null ? updated : 1L;
			}
			return getPendingDelta(postId);
		} catch (Exception ex) {
			log.warn("Redis post view tracking failed for postId={}", postId, ex);
			return 0L;
		}
	}

	@Override
	public long getPendingDelta(Long postId) {
		try {
			Object raw = stringRedisTemplate.opsForHash().get(DELTA_HASH_KEY, String.valueOf(postId));
			return raw == null ? 0L : Long.parseLong(raw.toString());
		} catch (Exception ex) {
			log.warn("Redis post view delta read failed for postId={}", postId, ex);
			return 0L;
		}
	}

	@Override
	public Map<Long, Long> getPendingDeltas(List<Long> postIds) {
		if (postIds == null || postIds.isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			List<Object> fields = postIds.stream().map(id -> (Object) String.valueOf(id)).toList();
			List<Object> values = stringRedisTemplate.opsForHash().multiGet(DELTA_HASH_KEY, fields);
			Map<Long, Long> result = new HashMap<>();
			for (int i = 0; i < postIds.size(); i++) {
				Object raw = values.get(i);
				if (raw != null) {
					long delta = Long.parseLong(raw.toString());
					if (delta > 0) {
						result.put(postIds.get(i), delta);
					}
				}
			}
			return result;
		} catch (Exception ex) {
			log.warn("Redis batch post view delta read failed", ex);
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<Long, Long> drainAllDeltas() {
		try {
			HashOperations<String, Object, Object> hashOps = stringRedisTemplate.opsForHash();
			Map<Object, Object> entries = hashOps.entries(DELTA_HASH_KEY);
			if (entries.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<Long, Long> drained = new HashMap<>();
			for (Map.Entry<Object, Object> entry : entries.entrySet()) {
				String field = entry.getKey().toString();
				long delta = Long.parseLong(entry.getValue().toString());
				if (delta > 0) {
					Long remaining = hashOps.increment(DELTA_HASH_KEY, field, -delta);
					if (remaining != null && remaining <= 0) {
						hashOps.delete(DELTA_HASH_KEY, field);
					}
					drained.put(Long.parseLong(field), delta);
				}
			}
			return drained;
		} catch (Exception ex) {
			log.warn("Redis post view delta drain failed", ex);
			return Collections.emptyMap();
		}
	}

}
