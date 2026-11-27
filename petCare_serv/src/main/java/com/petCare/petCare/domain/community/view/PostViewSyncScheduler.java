package com.petCare.petCare.domain.community.view;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.petCare.petCare.domain.community.service.CommunityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewSyncScheduler {

	private final CommunityService communityService;

	@Scheduled(fixedDelay = 60000)
	public void syncBufferedViewCounts() {
		int updated = communityService.flushBufferedViewsToDb();
		if (updated > 0) {
			log.debug("Synced buffered view counts for {} posts", updated);
		}
	}

}
