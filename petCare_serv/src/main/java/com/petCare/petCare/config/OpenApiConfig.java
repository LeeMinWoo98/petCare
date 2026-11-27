package com.petCare.petCare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI petCareOpenAPI() {
		String headerSchemeName = "MemberIdHeader";
		return new OpenAPI()
			.info(new Info()
				.title("PetCare REST API")
				.description("가족 단위 반려동물 공동 케어 서비스 API 명세서 (Step 6 전까지 X-Member-Id 헤더로 사용자 식별)")
				.version("v1"))
			.addSecurityItem(new SecurityRequirement().addList(headerSchemeName))
			.components(new Components().addSecuritySchemes(
				headerSchemeName,
				new SecurityScheme()
					.name("X-Member-Id")
					.type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.HEADER)
					.description("로그인/회원가입 후 발급된 memberId (Step 6에서 JWT Bearer로 교체 예정)")
			));
	}

}
