package com.petCare.petCare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.petCare.petCare.domain.community.dto.PostPageResponse;
import com.petCare.petCare.domain.community.entity.Post;
import com.petCare.petCare.domain.community.repository.PostRepository;
import com.petCare.petCare.domain.community.service.CommunityService;
import com.petCare.petCare.domain.community.view.InMemoryPostViewTracker;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Step4CommunityIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AuthService authService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private InMemoryPostViewTracker viewTracker;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		this.viewTracker.clearAll();
	}

	@Test
	@DisplayName("게시글 CRUD, 카테고리/키워드 검색, 댓글, 작성자 권한 검증 및 조회수 중복 방지/DB 반영이 동작한다")
	void communityFullFlow() throws Exception {
		AuthResponse author = authService.signup(new SignupRequest(
			"comm-author@petcare.test",
			"pass1234",
			"콩이언니",
			SignupMode.CREATE_FAMILY,
			"콩이네",
			null
		));
		AuthResponse reader = authService.signup(new SignupRequest(
			"comm-reader@petcare.test",
			"pass1234",
			"보리아빠",
			SignupMode.CREATE_FAMILY,
			"보리네",
			null
		));

		// 1. 비로그인 글 작성은 401
		mockMvc.perform(post("/api/v1/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"category\":\"INFO\",\"title\":\"테스트\",\"content\":\"본문\"}"))
			.andExpect(status().isUnauthorized());

		// 2. 게시글 2건 작성 (INFO, QUESTION)
		String infoPost = """
			{
			  "category": "INFO",
			  "title": "강아지 슬개골 관리 꿀팁",
			  "content": "미끄럼 방지 매트를 깔고 산책 후 스트레칭을 해주면 좋아요."
			}
			""";
		mockMvc.perform(post("/api/v1/posts")
				.header("X-Member-Id", author.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(infoPost))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.category").value("INFO"))
			.andExpect(jsonPath("$.authorNickname").value("콩이언니"));

		String questionPost = """
			{
			  "category": "QUESTION",
			  "title": "고양이 처방 사료 추천 부탁드려요",
			  "content": "요로결석 예방 사료 중 기호성 좋은 제품 있을까요?"
			}
			""";
		mockMvc.perform(post("/api/v1/posts")
				.header("X-Member-Id", reader.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(questionPost))
			.andExpect(status().isCreated());

		// 3. 비회원도 목록/검색/카테고리 필터 조회 가능
		mockMvc.perform(get("/api/v1/posts").param("category", "INFO").param("keyword", "슬개골"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].title").value("강아지 슬개골 관리 꿀팁"));

		PostPageResponse page = communityService.listPosts(null, "슬개골", 0, 10);
		Long infoPostId = page.content().getFirst().id();

		// 4. 조회수 중복 방지: 같은 사용자는 2번 봐도 1회, 다른 사용자가 보면 2회
		mockMvc.perform(get("/api/v1/posts/{postId}", infoPostId)
				.header("X-Member-Id", reader.memberId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.viewCount").value(1));

		mockMvc.perform(get("/api/v1/posts/{postId}", infoPostId)
				.header("X-Member-Id", reader.memberId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.viewCount").value(1));

		mockMvc.perform(get("/api/v1/posts/{postId}", infoPostId)
				.header("X-Member-Id", author.memberId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.viewCount").value(2));

		// 5. 버퍼링된 조회수 DB 반영 확인
		int flushed = communityService.flushBufferedViewsToDb();
		assertThat(flushed).isEqualTo(1);
		Post updatedInDb = postRepository.findActiveByIdWithAuthor(infoPostId).orElseThrow();
		assertThat(updatedInDb.getViewCount()).isEqualTo(2L);

		// 6. 댓글 작성 및 타인의 댓글 삭제 차단(403), 본인 삭제 성공(204)
		mockMvc.perform(post("/api/v1/posts/{postId}/comments", infoPostId)
				.header("X-Member-Id", reader.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"매트 정보 감사합니다!\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.authorNickname").value("보리아빠"));

		Long commentId = communityService.getPostDetail(infoPostId, "member:" + reader.memberId())
			.comments().getFirst().id();

		mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", infoPostId, commentId)
				.header("X-Member-Id", author.memberId()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", infoPostId, commentId)
				.header("X-Member-Id", reader.memberId()))
			.andExpect(status().isNoContent());

		// 7. 타인의 게시글 수정/삭제 차단(403) 후 작성자 본인 수정 & 삭제
		mockMvc.perform(put("/api/v1/posts/{postId}", infoPostId)
				.header("X-Member-Id", reader.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"category\":\"INFO\",\"title\":\"변조\",\"content\":\"변조\"}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/posts/{postId}", infoPostId)
				.header("X-Member-Id", author.memberId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"category\":\"INFO\",\"title\":\"강아지 슬개골 관리 꿀팁(수정)\",\"content\":\"본문 수정\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("강아지 슬개골 관리 꿀팁(수정)"));

		mockMvc.perform(delete("/api/v1/posts/{postId}", infoPostId)
				.header("X-Member-Id", author.memberId()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/posts/{postId}", infoPostId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
	}

}
