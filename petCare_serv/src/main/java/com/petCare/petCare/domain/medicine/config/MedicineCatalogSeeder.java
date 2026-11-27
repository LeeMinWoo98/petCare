package com.petCare.petCare.domain.medicine.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.petCare.petCare.domain.medicine.entity.MedicineCatalog;
import com.petCare.petCare.domain.medicine.repository.MedicineCatalogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 초기 구동 시 의약품 카탈로그가 비어 있으면 대표 동물용 의약품 샘플을 적재합니다.
 * 추후 공공데이터포털 CSV 파일 Import 배치를 통해 전체 품목을 확장 적재할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineCatalogSeeder implements ApplicationRunner {

	private final MedicineCatalogRepository catalogRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (catalogRepository.count() > 0) {
			return;
		}

		List<MedicineCatalog> defaults = List.of(
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-001")
				.name("하트가드 플러스 츄어블 (Heartgard Plus)")
				.manufacturer("베링거인겔하임")
				.efficacy("개 심장사상충 예방 및 회충·구충 구제")
				.dosageGuide("월 1회 체중에 맞는 용량 1정 경구 급여")
				.precautions("급여 전 심장사상충 감염 여부 확인 권장")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-002")
				.name("넥스가드 스펙트라 (NexGard Spectra)")
				.manufacturer("베링거인겔하임")
				.efficacy("내·외부 기생충(심장사상충, 벼룩, 진드기) 동시 예방")
				.dosageGuide("월 1회 체중 구간에 맞춰 1정 급여")
				.precautions("생후 8주령 이상, 체중 2kg 이상 반려견에 급여")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-003")
				.name("아포퀠정 (Apoquel)")
				.manufacturer("조에티스")
				.efficacy("반려견 알레르기성 피부염 및 아토피 소양증 완화")
				.dosageGuide("초기 14일간 1일 2회, 이후 1일 1회 수의사 처방 용량 급여")
				.precautions("12개월 미만 강아지 및 중증 감염 시 주의, 수의사 지시 필수")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-004")
				.name("브라벡토 츄어블 (Bravecto)")
				.manufacturer("MSD 동물약품")
				.efficacy("진드기 및 벼룩 구제(최대 12주 지속)")
				.dosageGuide("식사와 함께 또는 식후 12주마다 1정 경구 급여")
				.precautions("신경계 질환 병력이 있는 경우 수의사와 상담")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-005")
				.name("레볼루션 (Revolution)")
				.manufacturer("조에티스")
				.efficacy("개·고양이 심장사상충, 귀진드기, 벼룩 예방(스팟온)")
				.dosageGuide("월 1회 목 뒤 피부에 직접 도포")
				.precautions("도포 후 2시간 동안 목욕 및 접촉 주의")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-006")
				.name("세레니아정 (Cerenia)")
				.manufacturer("조에티스")
				.efficacy("반려견 구토 및 멀미 예방·치료")
				.dosageGuide("수의사 처방에 따라 1일 1회 급여 (멀미 시 출발 2시간 전)")
				.precautions("연속 복용 기간은 수의사 안내에 따름")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-007")
				.name("리마딜 츄어블 (Rimadyl)")
				.manufacturer("조에티스")
				.efficacy("반려견 골관절염 및 수술 후 통증·염증 완화")
				.dosageGuide("식후 1일 1~2회 수의사 처방 용량 급여")
				.precautions("위장 장애·구토 발생 시 즉시 중단 후 병원 방문")
				.build(),
			MedicineCatalog.builder()
				.externalCode("VET-SAMPLE-008")
				.name("프로바이오틱스 포티플로라 (FortiFlora)")
				.manufacturer("퓨리나")
				.efficacy("개·고양이 장 건강 및 소화 기능 개선 유산균")
				.dosageGuide("1일 1포 사료에 섞어 급여")
				.precautions("직사광선을 피해 서늘한 곳에 보관")
				.build()
		);

		catalogRepository.saveAll(defaults);
		log.info("Seeded {} default veterinary medicine catalog entries", defaults.size());
	}

}
