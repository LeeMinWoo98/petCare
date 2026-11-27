package com.petCare.petCare.domain.community.view;

import java.util.List;
import java.util.Map;

public interface PostViewTracker {

	/**
	 * 동일 열람자(24시간 TTL)가 처음 조회할 때만 조회수 버퍼를 1 증가시키고,
	 * 현재 DB 미반영 누적 증가치(delta)를 반환합니다.
	 */
	long recordViewAndGetDelta(Long postId, String viewerKey);

	long getPendingDelta(Long postId);

	Map<Long, Long> getPendingDeltas(List<Long> postIds);

	Map<Long, Long> drainAllDeltas();

}
