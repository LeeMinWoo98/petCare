package com.petCare.petCare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.petCare.petCare.domain.pet.dto.PetProfileResponse;
import com.petCare.petCare.domain.pet.dto.PetUpsertRequest;
import com.petCare.petCare.domain.pet.entity.PetGender;
import com.petCare.petCare.domain.pet.entity.PetSpecies;
import com.petCare.petCare.domain.pet.service.FamilyPetService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Step1FamilyAndPetIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AuthService authService;

	@Autowired
	private FamilyPetService familyPetService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	@DisplayName("가족 생성 회원가입 → 초대 코드 합류 → 내 가족 조회가 정상 동작한다")
	void familyOnboardingAndLookupFlow() throws Exception {
		AuthResponse owner = authService.signup(new SignupRequest(
			"mom@petcare.test",
			"pass1234",
			"엄마",
			SignupMode.CREATE_FAMILY,
			"콩이네 가족",
			null
		));

		assertThat(owner.inviteCode()).startsWith("PET-");
		assertThat(owner.familyOwner()).isTrue();

		String joinPayload = """
			{
			  "email": "dad@petcare.test",
			  "password": "pass1234",
			  "nickname": "아빠",
			  "mode": "JOIN_FAMILY",
			  "inviteCode": "%s"
			}
			""".formatted(owner.inviteCode());

		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinPayload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.familyId").value(owner.familyId()))
			.andExpect(jsonPath("$.familyOwner").value(false))
			.andExpect(jsonPath("$.inviteCode").value(owner.inviteCode()));

		String loginPayload = """
			{
			  "email": "dad@petcare.test",
			  "password": "pass1234"
			}
			""";

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginPayload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyId").value(owner.familyId()))
			.andExpect(jsonPath("$.nickname").value("아빠"));

		mockMvc.perform(get("/api/v1/families/me")
				.header("X-Member-Id", owner.memberId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("콩이네 가족"))
			.andExpect(jsonPath("$.inviteCode").value(owner.inviteCode()))
			.andExpect(jsonPath("$.members.length()").value(2));
	}

	@Test
	@DisplayName("가족 구성원은 같은 반려동물 CRUD를 공유하고, 다른 가족은 접근할 수 없다")
	void familyPetCrudAndCrossFamilyIsolation() throws Exception {
		AuthResponse familyAOwner = authService.signup(new SignupRequest(
			"a-owner@petcare.test",
			"pass1234",
			"A대표",
			SignupMode.CREATE_FAMILY,
			"A가족",
			null
		));
		AuthResponse familyAMember = authService.signup(new SignupRequest(
			"a-member@petcare.test",
			"pass1234",
			"A구성원",
			SignupMode.JOIN_FAMILY,
			null,
			familyAOwner.inviteCode()
		));
		AuthResponse familyBOwner = authService.signup(new SignupRequest(
			"b-owner@petcare.test",
			"pass1234",
			"B대표",
			SignupMode.CREATE_FAMILY,
			"B가족",
			null
		));

		// 1. 미인증 요청은 401
		mockMvc.perform(get("/api/v1/pets"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		// 2. A가족 대표가 반려동물 등록
		String createBody = """
			{
			  "name": "콩이",
			  "species": "DOG",
			  "breed": "말티즈",
			  "birthDate": "2022-05-10",
			  "gender": "FEMALE",
			  "neutered": true,
			  "weightKg": 3.45,
			  "notes": "닭고기 알러지 주의"
			}
			""";

		mockMvc.perform(post("/api/v1/pets")
				.header("X-Member-Id", familyAOwner.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(createBody))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("콩이"))
			.andExpect(jsonPath("$.species").value("DOG"))
			.andExpect(jsonPath("$.familyId").value(familyAOwner.familyId()));

		// 3. 같은 가족 구성원(A구성원)도 목록 조회 가능
		mockMvc.perform(get("/api/v1/pets")
				.header("X-Member-Id", familyAMember.memberId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].name").value("콩이"));

		PetProfileResponse created = familyPetService.listFamilyPets(familyAOwner.memberId()).getFirst();

		// 4. 다른 가족(B대표)은 A가족 반려동물 조회/수정/삭제 불가 (404 PET_NOT_FOUND)
		mockMvc.perform(get("/api/v1/pets/{petId}", created.id())
				.header("X-Member-Id", familyBOwner.memberId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));

		// 5. 같은 가족 구성원(A구성원)이 프로필 수정
		String updateBody = """
			{
			  "name": "콩이",
			  "species": "DOG",
			  "breed": "말티즈",
			  "birthDate": "2022-05-10",
			  "gender": "FEMALE",
			  "neutered": true,
			  "weightKg": 3.60,
			  "notes": "산책 좋아함"
			}
			""";

		mockMvc.perform(put("/api/v1/pets/{petId}", created.id())
				.header("X-Member-Id", familyAMember.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.weightKg").value(3.6))
			.andExpect(jsonPath("$.notes").value("산책 좋아함"));

		// 6. 소프트 삭제 후 목록 및 단건 조회에서 제외됨
		mockMvc.perform(delete("/api/v1/pets/{petId}", created.id())
				.header("X-Member-Id", familyAOwner.memberId()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/pets")
				.header("X-Member-Id", familyAOwner.memberId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/v1/pets/{petId}", created.id())
				.header("X-Member-Id", familyAOwner.memberId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));
	}

	@Test
	@DisplayName("반려동물 등록 시 성별을 생략하면 UNKNOWN으로 기본 저장된다")
	void defaultGenderIsUnknown() {
		AuthResponse owner = authService.signup(new SignupRequest(
			"cat-owner@petcare.test",
			"pass1234",
			"집사",
			SignupMode.CREATE_FAMILY,
			"치즈네",
			null
		));

		PetProfileResponse pet = familyPetService.registerPet(
			owner.memberId(),
			new PetUpsertRequest(
				"치즈",
				PetSpecies.CAT,
				"코리안숏헤어",
				LocalDate.of(2023, 3, 1),
				null,
				false,
				new BigDecimal("4.20"),
				"  "
			)
		);

		assertThat(pet.gender()).isEqualTo(PetGender.UNKNOWN);
		assertThat(pet.notes()).isNull();
	}

}
