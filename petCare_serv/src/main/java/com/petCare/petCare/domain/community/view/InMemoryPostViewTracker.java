package com.petCare.petCare.domain.community.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryPostViewTracker implements PostViewTracker {

	private final Set<String> seenKeys = ConcurrentHashMap.newKeySet();
	private final Map<Long, LongAdder> deltas = new ConcurrentHashMap<>();

	@Override
	public long recordViewAndGetDelta(Long postId, String viewerKey) {
		String seenKey = postId + ":" + viewerKey;
		if (seenKeys.add(seenKey)) {
			deltas.computeIfAbsent(postId, key -> new LongAdder()).increment();
		}
		return getPendingDelta(postId);
	}

	@Override
	public long getPendingDelta(Long postId) {
		LongAdder adder = deltas.get(postId);
		return adder == null ? 0L : adder.sum();
	}

	@Override
	public Map<Long, Long> getPendingDeltas(List<Long> postIds) {
		Map<Long, Long> result = new HashMap<>();
		for (Long postId : postIds) {
			long delta = getPendingDelta(postId);
			if (delta > 0) {
				result.put(postId, delta);
			}
		}
		return result;
	}

	@Override
	public Map<Long, Long> drainAllDeltas() {
		Map<Long, Long> drained = new HashMap<>();
		for (Long postId : List.copyOf(deltas.keySet())) {
			LongAdder removed = deltas.remove(postId);
			if (removed != null) {
				long sum = removed.sum();
				if (sum > 0) {
					drained.put(postId, sum);
				}
			}
		}
		return drained;
	}

	public void clearAll() {
		seenKeys.clear();
		deltas.clear();
	}

}
