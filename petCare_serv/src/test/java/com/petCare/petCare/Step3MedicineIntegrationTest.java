package com.petCare.petCare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

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
import com.petCare.petCare.domain.medicine.dto.MedicineCatalogItemResponse;
import com.petCare.petCare.domain.medicine.dto.PetMedicineResponse;
import com.petCare.petCare.domain.medicine.lock.MedicationLockManager;
import com.petCare.petCare.domain.medicine.service.MedicineCatalogService;
import com.petCare.petCare.domain.medicine.service.PetMedicineService;
import com.petCare.petCare.domain.pet.dto.PetProfileResponse;
import com.petCare.petCare.domain.pet.dto.PetUpsertRequest;
import com.petCare.petCare.domain.pet.entity.PetGender;
import com.petCare.petCare.domain.pet.entity.PetSpecies;
import com.petCare.petCare.domain.pet.service.FamilyPetService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Step3MedicineIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AuthService authService;

	@Autowired
	private FamilyPetService familyPetService;

	@Autowired
	private MedicineCatalogService catalogService;

	@Autowired
	private PetMedicineService petMedicineService;

	@Autowired
	private MedicationLockManager lockManager;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	@DisplayName("의약품 카탈로그 검색 → 약 등록(API/CUSTOM) → 투약 체크 시 잔여량·잔여일수 차감 및 취소 복구가 동작한다")
	void medicineCatalogAndDoseLifecycle() throws Exception {
		// 1. 초기 시드된 카탈로그 검색 확인
		mockMvc.perform(get("/api/v1/medicines/search").param("keyword", "아포퀠"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].name").value("아포퀠정 (Apoquel)"));

		List<MedicineCatalogItemResponse> found = catalogService.search("아포퀠");
		Long catalogId = found.getFirst().id();

		AuthResponse mom = authService.signup(new SignupRequest(
			"med-mom@petcare.test",
			"pass1234",
			"엄마",
			SignupMode.CREATE_FAMILY,
			"초코네",
			null
		));
		AuthResponse dad = authService.signup(new SignupRequest(
			"med-dad@petcare.test",
			"pass1234",
			"아빠",
			SignupMode.JOIN_FAMILY,
			null,
			mom.inviteCode()
		));
		AuthResponse stranger = authService.signup(new SignupRequest(
			"med-stranger@petcare.test",
			"pass1234",
			"타인",
			SignupMode.CREATE_FAMILY,
			"남의집",
			null
		));

		PetProfileResponse pet = familyPetService.registerPet(mom.memberId(), new PetUpsertRequest(
			"초코",
			PetSpecies.DOG,
			"비숑",
			LocalDate.of(2020, 4, 10),
			PetGender.FEMALE,
			true,
			new BigDecimal("4.80"),
			"피부 알레르기"
		));

		// 2. 카탈로그 기반 약 등록 (1일 2회, 총 6회 = 3일치 → 소진 임박 true)
		String apiMedPayload = """
			{
			  "sourceType": "API",
			  "catalogId": %d,
			  "name": "아포퀠정 (Apoquel)",
			  "dosageMethod": "아침/저녁 식후 반 알씩 간식에 싸서 급여",
			  "dailyDoseCount": 2,
			  "totalDoses": 6,
			  "startDate": "2026-11-22"
			}
			""".formatted(catalogId);

		mockMvc.perform(post("/api/v1/pets/{petId}/medicines", pet.id())
				.header("X-Member-Id", mom.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(apiMedPayload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sourceType").value("API"))
			.andExpect(jsonPath("$.remainingDoses").value(6))
			.andExpect(jsonPath("$.remainingDays").value(3))
			.andExpect(jsonPath("$.lowStock").value(true));

		// 3. 직접 입력(CUSTOM) 약 등록 (1일 1회, 총 10회 = 10일치 → 소진 임박 false)
		String customMedPayload = """
			{
			  "sourceType": "CUSTOM",
			  "name": "동물병원 처방 안약",
			  "dosageMethod": "양쪽 눈에 1방울씩 점안",
			  "dailyDoseCount": 1,
			  "totalDoses": 10,
			  "startDate": "2026-11-22"
			}
			""";

		mockMvc.perform(post("/api/v1/pets/{petId}/medicines", pet.id())
				.header("X-Member-Id", dad.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(customMedPayload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sourceType").value("CUSTOM"))
			.andExpect(jsonPath("$.remainingDays").value(10))
			.andExpect(jsonPath("$.lowStock").value(false));

		List<PetMedicineResponse> list = petMedicineService.listPetMedicines(
			mom.memberId(), pet.id(), LocalDate.of(2026, 11, 22)
		);
		assertThat(list).hasSize(2);
		PetMedicineResponse apoquel = list.stream()
			.filter(m -> m.name().contains("아포퀠"))
			.findFirst()
			.orElseThrow();

		// 4. 동시 클릭 분산 락 충돌 테스트 (락 선점 시 409 CONFLICT 반환)
		String heldToken = lockManager.tryLock(apoquel.id(), Duration.ofSeconds(5));
		assertThat(heldToken).isNotNull();
		try {
			mockMvc.perform(post("/api/v1/pets/{petId}/medicines/{medicineId}/logs", pet.id(), apoquel.id())
					.header("X-Member-Id", dad.memberId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"medicatedAt\":\"2026-11-22T09:00:00\",\"memo\":\"아침 투약\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONCURRENT_MEDICATION_REQUEST"));
		} finally {
			lockManager.unlock(apoquel.id(), heldToken);
		}

		// 5. 락 해제 후 정상 투약 체크 2회 → remainingDoses: 6 -> 4, remainingDays: 2, todayCompleted: true
		mockMvc.perform(post("/api/v1/pets/{petId}/medicines/{medicineId}/logs", pet.id(), apoquel.id())
				.header("X-Member-Id", mom.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"medicatedAt\":\"2026-11-22T09:00:00\",\"memo\":\"아침 급여\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberNickname").value("엄마"));

		mockMvc.perform(post("/api/v1/pets/{petId}/medicines/{medicineId}/logs", pet.id(), apoquel.id())
				.header("X-Member-Id", dad.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"medicatedAt\":\"2026-11-22T20:00:00\",\"memo\":\"저녁 급여\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberNickname").value("아빠"));

		mockMvc.perform(get("/api/v1/pets/{petId}/medicines", pet.id())
				.header("X-Member-Id", dad.memberId())
				.param("date", "2026-11-22"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].remainingDoses").value(4))
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].remainingDays").value(2))
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].todayDoseCount").value(2))
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].todayCompleted").value(true));

		PetMedicineResponse refreshed = petMedicineService.listPetMedicines(
			mom.memberId(), pet.id(), LocalDate.of(2026, 11, 22)
		).stream().filter(m -> m.id().equals(apoquel.id())).findFirst().orElseThrow();
		Long latestLogId = refreshed.todayLogs().getFirst().id();

		// 6. 타 가족은 약 조회·투약 취소 불가 (404 격리)
		mockMvc.perform(get("/api/v1/pets/{petId}/medicines", pet.id())
				.header("X-Member-Id", stranger.memberId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));

		mockMvc.perform(delete("/api/v1/medication-logs/{logId}", latestLogId)
				.header("X-Member-Id", stranger.memberId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEDICATION_LOG_NOT_FOUND"));

		// 7. 가족이 투약 취소 시 잔여량 복구 (4 -> 5, remainingDays: ceil(5/2) = 3)
		mockMvc.perform(delete("/api/v1/medication-logs/{logId}", latestLogId)
				.header("X-Member-Id", mom.memberId()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/pets/{petId}/medicines", pet.id())
				.header("X-Member-Id", mom.memberId())
				.param("date", "2026-11-22"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].remainingDoses").value(5))
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].remainingDays").value(3))
			.andExpect(jsonPath("$[?(@.id == " + apoquel.id() + ")].todayCompleted").value(false));

		// 8. 약 수정(잔여량 0으로 변경) 후 투약 시도 시 400 NO_REMAINING_DOSES
		String depleteUpdate = """
			{
			  "name": "아포퀠정 (Apoquel)",
			  "dosageMethod": "식후 급여",
			  "dailyDoseCount": 2,
			  "totalDoses": 6,
			  "remainingDoses": 0,
			  "startDate": "2026-11-22",
			  "active": true
			}
			""";
		mockMvc.perform(put("/api/v1/pets/{petId}/medicines/{medicineId}", pet.id(), apoquel.id())
				.header("X-Member-Id", mom.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(depleteUpdate))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.depleted").value(true));

		mockMvc.perform(post("/api/v1/pets/{petId}/medicines/{medicineId}/logs", pet.id(), apoquel.id())
				.header("X-Member-Id", mom.memberId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("NO_REMAINING_DOSES"));
	}

}
