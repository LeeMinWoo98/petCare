package com.petCare.petCare.domain.community.dto;

import java.util.List;

public record PostPageResponse(
	List<PostSummaryResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
}
