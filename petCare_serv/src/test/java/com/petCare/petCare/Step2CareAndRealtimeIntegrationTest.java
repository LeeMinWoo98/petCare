package com.petCare.petCare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.petCare.petCare.domain.auth.dto.AuthResponse;
import com.petCare.petCare.domain.auth.dto.SignupMode;
import com.petCare.petCare.domain.auth.dto.SignupRequest;
import com.petCare.petCare.domain.auth.service.AuthService;
import com.petCare.petCare.domain.care.cache.InMemoryDailyCareCacheStore;
import com.petCare.petCare.domain.care.dto.DailyCareSummaryResponse;
import com.petCare.petCare.domain.care.service.DailyCareService;
import com.petCare.petCare.domain.pet.dto.PetProfileResponse;
import com.petCare.petCare.domain.pet.dto.PetUpsertRequest;
import com.petCare.petCare.domain.pet.entity.PetGender;
import com.petCare.petCare.domain.pet.entity.PetSpecies;
import com.petCare.petCare.domain.pet.service.FamilyPetService;
import com.petCare.petCare.domain.realtime.service.FamilySseRegistry;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Step2CareAndRealtimeIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AuthService authService;

	@Autowired
	private FamilyPetService familyPetService;

	@Autowired
	private DailyCareService dailyCareService;

	@Autowired
	private InMemoryDailyCareCacheStore cacheStore;

	@Autowired
	private FamilySseRegistry sseRegistry;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		this.cacheStore.clearAll();
	}

	@Test
	@DisplayName("같은 가족 구성원끼리 당일 산책·식사·간식 기록을 공유하고 캐시 무효화 및 가족 격리가 동작한다")
	void careLogSharingCacheEvictionAndIsolation() throws Exception {
		AuthResponse mom = authService.signup(new SignupRequest(
			"care-mom@petcare.test",
			"pass1234",
			"엄마",
			SignupMode.CREATE_FAMILY,
			"보리네",
			null
		));
		AuthResponse dad = authService.signup(new SignupRequest(
			"care-dad@petcare.test",
			"pass1234",
			"아빠",
			SignupMode.JOIN_FAMILY,
			null,
			mom.inviteCode()
		));
		AuthResponse stranger = authService.signup(new SignupRequest(
			"care-stranger@petcare.test",
			"pass1234",
			"다른가족",
			SignupMode.CREATE_FAMILY,
			"남의집",
			null
		));

		PetProfileResponse pet = familyPetService.registerPet(mom.memberId(), new PetUpsertRequest(
			"보리",
			PetSpecies.DOG,
			"푸들",
			LocalDate.of(2021, 8, 15),
			PetGender.MALE,
			true,
			new BigDecimal("5.10"),
			null
		));

		// 1. 초기 조회 (캐시 적재)
		mockMvc.perform(get("/api/v1/pets/{petId}/care-logs", pet.id())
				.header("X-Member-Id", mom.memberId())
				.param("date", "2026-11-22"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.walkCount").value(0))
			.andExpect(jsonPath("$.mealCount").value(0))
			.andExpect(jsonPath("$.snackCount").value(0))
			.andExpect(jsonPath("$.logs.length()").value(0));

		assertThat(cacheStore.get(mom.familyId(), pet.id(), LocalDate.of(2026, 11, 22))).isPresent();

		// 2. 엄마가 아침 식사 기록, 아빠가 산책 기록 추가 (캐시 자동 무효화)
		String mealPayload = """
			{
			  "careType": "MEAL",
			  "performedAt": "2026-11-22T08:30:00",
			  "memo": "아침 사료 60g 완밥"
			}
			""";
		mockMvc.perform(post("/api/v1/pets/{petId}/care-logs", pet.id())
				.header("X-Member-Id", mom.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(mealPayload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.careType").value("MEAL"))
			.andExpect(jsonPath("$.memberNickname").value("엄마"));

		String walkPayload = """
			{
			  "careType": "WALK",
			  "performedAt": "2026-11-22T18:10:00",
			  "memo": "공원 산책 35분"
			}
			""";
		mockMvc.perform(post("/api/v1/pets/{petId}/care-logs", pet.id())
				.header("X-Member-Id", dad.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(walkPayload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.careType").value("WALK"))
			.andExpect(jsonPath("$.memberNickname").value("아빠"));

		// 3. 아빠가 조회 시 최신 2건(최신 수행 시각 순) 반영
		mockMvc.perform(get("/api/v1/pets/{petId}/care-logs", pet.id())
				.header("X-Member-Id", dad.memberId())
				.param("date", "2026-11-22"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.walkCount").value(1))
			.andExpect(jsonPath("$.mealCount").value(1))
			.andExpect(jsonPath("$.snackCount").value(0))
			.andExpect(jsonPath("$.logs.length()").value(2))
			.andExpect(jsonPath("$.logs[0].careType").value("WALK"))
			.andExpect(jsonPath("$.logs[1].careType").value("MEAL"));

		DailyCareSummaryResponse summary = dailyCareService.getDailyCareSummary(
			mom.memberId(), pet.id(), LocalDate.of(2026, 11, 22)
		);
		Long walkLogId = summary.logs().getFirst().id();

		// 4. 다른 가족은 조회 및 삭제 불가 (404 격리)
		mockMvc.perform(get("/api/v1/pets/{petId}/care-logs", pet.id())
				.header("X-Member-Id", stranger.memberId())
				.param("date", "2026-11-22"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));

		mockMvc.perform(delete("/api/v1/care-logs/{logId}", walkLogId)
				.header("X-Member-Id", stranger.memberId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CARE_LOG_NOT_FOUND"));

		// 5. 같은 가족(엄마)이 산책 기록 취소 → 즉시 반영
		mockMvc.perform(delete("/api/v1/care-logs/{logId}", walkLogId)
				.header("X-Member-Id", mom.memberId()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/pets/{petId}/care-logs", pet.id())
				.header("X-Member-Id", dad.memberId())
				.param("date", "2026-11-22"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.walkCount").value(0))
			.andExpect(jsonPath("$.mealCount").value(1))
			.andExpect(jsonPath("$.logs.length()").value(1));
	}

	@Test
	@DisplayName("가족 실시간 SSE 구독 엔드포인트가 연결되고 미인증 요청은 차단된다")
	void sseSubscriptionFlow() throws Exception {
		AuthResponse owner = authService.signup(new SignupRequest(
			"sse-owner@petcare.test",
			"pass1234",
			"구독자",
			SignupMode.CREATE_FAMILY,
			"실시간네",
			null
		));

		mockMvc.perform(get("/api/v1/families/me/events"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/families/me/events")
				.param("memberId", String.valueOf(owner.memberId()))
				.accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andExpect(header().string("X-Accel-Buffering", "no"));

		assertThat(sseRegistry.activeConnectionCount(owner.familyId())).isGreaterThanOrEqualTo(1);
	}

}
