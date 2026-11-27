# PetCare 서비스 설계서

> **가족 단위 반려동물 공동 케어 & 건강 정보 플랫폼**  
> 이 문서는 서비스 기획부터 DB·API·화면 설계까지 한눈에 보고, 단계적으로 코드를 구현하기 위한 기준 설계서입니다.  
> 각 항목은 고유 ID(`F-001`, `P-001`, `API-001` 등)로 서로 연결되어 있습니다.

---

## 0. 기술 스택 & 환경 (현재 프로젝트 기준)

| 영역 | 기술 스택 | 폴더 / 비고 |
| --- | --- | --- |
| Frontend | Vue 3 (`<script setup>`), Vite, JavaScript, Vue Router, Tailwind CSS v4, **PrimeVue 5 (Aura 테마 + PrimeIcons)** | `petCare_web/` (기본 포트 `5173`), 모바일 세로형 App Shell |
| Backend | Spring Boot 4.1.2, Java 21, Spring Data JPA, Spring Security, Validation, REST API (`springdoc-openapi` Swagger) | `petCare_serv/` (기본 포트 `8080`) |
| Database | **MariaDB** (`utf8mb4`, `Asia/Seoul`) | Docker Compose로 로컬/배포 환경 통일 |
| Cache / Lock / PubSub | **Redis** | 당일 케어 캐싱, 투약 중복 클릭 분산 락, SSE Pub/Sub, JWT Refresh Token, 조회수 버퍼링, AI Rate Limit |
| Realtime Sync | **SSE (Server-Sent Events)** + Redis Pub/Sub | 가족 간 산책·식사·간식·투약 체크 실시간 화면 반영 (`SseEmitter` + `EventSource`) |
| Scheduler / Crawler | **Spring `@Scheduled`** + Redis 분산 락(ShedLock), Jsoup/RSS 크롤러 | Spring Batch 미사용 (중소 규모 CSV/크롤링에 경량 스케줄러 채택) |
| Observability / Logging | **JSON Logback(MDC)** + **Actuator / Micrometer** + **PLG 스택 (Prometheus + Grafana + Loki + Promtail)** | `traceId`·`familyId` 기반 로그 추적 + API/DB/Redis 메트릭 대시보드 |
| Infra / Container | **Docker Compose** (기본: MariaDB + Redis, `--profile monitoring`: PLG 스택) | 로컬 PC 메모리 절약 및 배포 환경 일치 |
| Auth | 초기 개발 단계 전체 허용(`permitAll`), 추후 JWT(+ Redis Refresh Token) 적용 예정 | |
| External Data | 동물용 의약품 데이터(공공데이터포털 CSV 적재 + 수동 입력 지원), 건강·신약 기사 크롤러 | |
| AI / LLM (추후) | 외부 LLM API (Gemini Flash 또는 OpenAI GPT-4o-mini 등) | 핵심 개발 완료 후 Step 7에서 백엔드 중계 방식으로 연동 |
| App Packaging (추후) | PWA (`vite-plugin-pwa`) 또는 Capacitor 기반 Android/iOS 하이브리드 앱 전환 고려 | 처음부터 모바일 세로형 App Shell 구조로 설계 |
| Deploy | Docker Compose 기반 서버 배포 *(세부 호스팅 추후 결정)* | |

---

## 1. 서비스 개요 (Overview)

- **서비스 한 줄 요약**: 가족 구성원이 하나의 그룹으로 묶여 반려동물의 일상 케어(산책·식사·간식)와 투약 현황(급여 여부·잔여 일수)을 실시간으로 공유하고, 반려동물 정보 게시판과 관리자 검증 건강·신약 콘텐츠를 함께 이용하는 서비스
- **타겟 사용자**: 반려동물을 함께 돌보는 가족 단위 보호자 (부모·자녀·부부·동거인 등)
- **핵심 가치 / 해결할 문제**:
  - 가족 간 소통 누락으로 발생하는 **사료·간식 중복 급여** 및 **투약 누락/중복 투약** 방지
  - 처방약의 **급여 방법 공유** 및 **잔여 일수(며칠치 남았는지) 자동 계산**으로 재처방 시점 놓침 방지
  - 보호자 간 **정보 공유 커뮤니티** + 관리자가 검증한 **신뢰할 수 있는 질병·신약·건강 관리 정보** 제공

### MVP 범위 (1차 목표 기준)
- **포함 (In Scope)**
  - [x] 가족 그룹 생성(고유 초대 번호 발급) 및 초대 번호 입력을 통한 가족 합류 회원가입
  - [x] 가족 단위 반려동물 기본 정보 등록/조회/수정/삭제
  - [x] 접속 당일 기준 산책·간식·식사 기록 및 가족 단위 실시간 공유
  - [x] 동물 의약품 API/카탈로그 검색 우선 제공 + 미존재 시 사용자 직접 등록, 급여 방법 저장
  - [x] 오늘 투약 여부 체크, 약 잔여 일수(며칠치) 계산·표시 및 가족 공유
  - [x] SSE + Redis Pub/Sub 기반 가족 간 당일 케어·투약 실시간 화면 동기화
  - [x] 반려동물 정보 공유 커뮤니티 게시판 (글/댓글)
  - [ ] 질병·신약·건강 관리법 크롤링 수집 → 관리자 검수/승인 → 승인 콘텐츠 공개 공간
- **제외 (Out of Scope / 추후 고도화)**
  - [ ] 모바일 백그라운드 푸시 알림(FCM / 웹 푸시) 및 카카오 알림톡
  - [ ] 소셜 로그인(카카오/구글) 및 다중 가족 그룹 소속 기능
  - [ ] 병원 예약 / 가계부(지출 관리)

---

## 2. 사용자 역할 & 권한 (Roles)

| 역할 | 코드 | 설명 | 주요 가능 작업 | 비고 |
| --- | --- | --- | --- | --- |
| 비회원 | `GUEST` | 로그인 전 사용자 | 서비스 소개, 승인된 건강·신약 정보 열람, 커뮤니티 글 읽기(정책에 따라 조정), 회원가입/로그인 | |
| 가족 구성원 (일반 회원) | `USER` | 가족 그룹에 속한 보호자 | 반려동물 관리, 당일 산책/식사/간식·투약 기록 및 조회(본인 가족 한정), 약 등록/수정, 커뮤니티 글·댓글 작성 | 첫 가입자는 가족 생성자(`FAMILY_OWNER` 플래그 또는 역할 구분 가능) |
| 홈페이지 관리자 | `ADMIN` | 서비스 운영자 | 크롤링 수동/자동 실행 관리, 수집된 기사·정보 검수(승인/반려/수정/삭제), 커뮤니티 부적절 게시글 관리 | |

---

## 3. 기능 목록 (Features)

> 우선순위: `P0`(핵심 MVP), `P1`(주요 기능), `P2`(운영/확장 기능)

| ID | 우선순위 | 대분류 | 기능명 | 상세 설명 | 완료 조건 (Acceptance Criteria) |
| --- | --- | --- | --- | --- | --- |
| `F-001` | P0 | 회원/가족 | 신규 가족 생성 가입 | 첫 가입자가 회원가입 시 고유 가족 번호(초대 코드)를 자동 발급받고 가족 그룹이 생성됨 | 가입 완료 시 중복 없는 고유 번호(예: 8자리 영문+숫자)가 발급되고 마이페이지/대시보드에서 복사 가능 |
| `F-002` | P0 | 회원/가족 | 가족 번호로 합류 가입 | 나머지 가족이 회원가입 시 발급받은 고유 번호를 입력해 기존 가족 그룹에 자동 소속됨 | 유효한 번호 입력 시 해당 가족 그룹에 연결, 잘못된 번호 입력 시 명확한 에러 메시지 표시 |
| `F-003` | P0 | 회원/가족 | 로그인 / 가족 구성원 조회 | 로그인 후 소속된 가족 정보와 구성원 목록 확인 | 로그인한 사용자의 `familyId` 기준으로 모든 케어 데이터가 자동 스코핑됨 |
| `F-004` | P0 | 반려동물 | 반려동물 기본 정보 CRUD | 가족이 함께 돌보는 반려동물의 이름, 종(개/고양이 등), 품종, 생년월일, 성별, 몸무게, 중성화 여부, 특이사항 등록/수정/삭제 | 같은 가족 구성원이라면 누구나 등록된 반려동물 목록을 조회하고 수정 가능 |
| `F-005` | P0 | 데일리 케어 | 당일 산책·식사·간식 기록 및 공유 | 접속 당일 기준으로 산책/식사/간식 수행 여부, 시간, 담당 가족(기록자), 메모를 기록하고 가족 전체에 공유 | 당일 화면에서 반려동물별 산책/식사/간식 완료 여부와 "누가 몇 시에 기록했는지" 즉시 확인 가능 |
| `F-006` | P0 | 투약 관리 | 약 검색(외부 API/카탈로그) + 직접 등록 | 동물 의약품 정보 API(또는 공공데이터 적재 DB)에서 약 검색을 먼저 제공하고, 검색 결과가 없을 경우 사용자가 약 이름·성분/용도·급여 방법을 직접 등록 | API 검색 결과 선택 시 기본 정보가 자동 입력되고, 없으면 "직접 등록" 폼으로 자연스럽게 전환 |
| `F-007` | P0 | 투약 관리 | 급여 방법 및 처방량 관리 | 약별 급여 방법(용량, 1일 급여 횟수, 식전/식후, 주의사항)과 총 처방량(총 일수 또는 총 투여 횟수) 저장 | 저장 후 가족 모두가 동일한 급여 방법을 확인 가능 |
| `F-008` | P0 | 투약 관리 | 오늘 투약 체크 & 잔여 일수 알림 | 해당 약의 오늘 급여 여부를 가족 단위로 체크·공유하고, 남은 투약량 기준으로 **며칠치 남았는지** 자동 계산하여 표시 | 투약 체크 시 잔여량/잔여 일수가 갱신되며, 소진 임박(예: 3일 이하) 시 시각적 경고 배지 표시 |
| `F-009` | P1 | 커뮤니티 | 반려동물 정보 공유 게시판 | 보호자들이 팁·후기·질문 등 반려동물 정보를 공유하는 게시판 (카테고리, 검색, 글/댓글 CRUD) | 글 목록 페이징·검색, 작성자 본인만 수정/삭제 가능, 댓글 작성·삭제 지원 |
| `F-010` | P1 | 건강 정보 | 질병·신약·건강 관리법 크롤링 수집 | 외부 뉴스/공식 사이트/RSS 등에서 반려동물 질병·신약·건강 관리법 기사를 수집하여 `PENDING(승인 대기)` 상태로 저장 | 중복 URL 수집 방지, 제목·요약·원문 출처 URL·수집일시 저장 |
| `F-011` | P1 | 관리자 | 크롤링 콘텐츠 검수 및 승인 | 관리자가 수집된 대기 목록을 확인하고 내용 보완 후 `APPROVED(승인)` 또는 `REJECTED(반려)` 처리 | `ADMIN` 권한만 접근 가능, 승인 처리 즉시 공개 공간에 노출됨 |
| `F-012` | P1 | 건강 정보 | 승인된 건강·신약 정보 열람 공간 | 관리자가 승인한 질병·신약·건강 관리법 기사와 정보를 카테고리별로 열람하고 원문 링크 확인 | 승인(`APPROVED`) 상태의 항목만 사용자 화면에 노출, 검색/카테고리 필터 지원 |
| `F-013` | P2 | AI Q&A | 반려동물 음식·건강 AI 질문 (외부 LLM) | 먹어도 되는 음식, 먹으면 안 되는 음식, 건강에 좋은 음식 등을 질문하면 등록된 반려동물 정보(종·나이·몸무게·특이사항)를 반영해 안전 여부(`안전/주의/금지`)와 이유·급여 팁을 답변 | 핵심 개발 완료 후 Step 7 구현, 백엔드 중계 호출·일일 횟수 제한·면책 문구 고정 표시 |
| `F-014` | P1 | 실시간 공유 | 가족 케어·투약 SSE 실시간 동기화 | 한 가족이 산책·식사·간식·투약을 기록/취소하면 같은 `familyId`로 접속 중인 다른 가족의 화면에 SSE(+ Redis Pub/Sub)로 즉시 반영 및 토스트 알림 표시 | 새로고침 없이 타 가족 화면 상태 즉시 갱신, 연결 끊김 시 자동 재연결 |

---

## 4. 화면 설계 & 라우팅 (Pages & User Flow)

### 4-1. 페이지 목록 (`petCare_web/src/views`)

| 페이지 ID | Route 경로 | View 파일명(예정) | 관련 기능 ID | 접근 권한 | 화면 목적 및 주요 UI 요소 |
| --- | --- | --- | --- | --- | --- |
| `P-001` | `/` | `HomeView.vue` | `F-004`, `F-005`, `F-008` | `USER` (비회원은 소개/로그인 유도) | **오늘의 가족 케어 대시보드** — 반려동물 선택 탭, 오늘의 산책/식사/간식 체크 카드(기록자·시간 표시), 오늘 먹여야 할 약 체크 + 잔여 일수 배지, 가족 초대 코드 복사 버튼 |
| `P-002` | `/login` | `LoginView.vue` | `F-003` | `GUEST` | 로그인 폼 (이메일/아이디, 비밀번호) |
| `P-003` | `/signup` | `SignupView.vue` | `F-001`, `F-002` | `GUEST` | 회원가입 폼 — **"새 가족 만들기" / "가족 코드로 참여하기"** 탭 전환, 가족 코드 입력란 |
| `P-004` | `/pets` | `PetListView.vue` | `F-004` | `USER` | 가족의 반려동물 목록, 등록 버튼, 각 반려동물 기본 정보 수정/삭제 진입 |
| `P-005` | `/pets/new`, `/pets/:id/edit` | `PetFormView.vue` | `F-004` | `USER` | 반려동물 기본 정보 등록/수정 폼 |
| `P-006` | `/pets/:id/medicines` | `MedicineListView.vue` | `F-006`, `F-007`, `F-008` | `USER` | 반려동물별 복용 중인 약 목록, 오늘 급여 체크, 잔여 일수(D-day/며칠치) 표시, 급여 방법 상세 모달/카드 |
| `P-007` | `/pets/:id/medicines/new` | `MedicineFormView.vue` | `F-006`, `F-007` | `USER` | **1단계: 동물 의약품 API 검색** → 검색 결과 선택 시 자동 채움 / 결과 없을 시 **2단계: 직접 입력 폼**(약 이름, 급여 방법, 1일 횟수, 총 일수/개수) |
| `P-008` | `/community` | `CommunityListView.vue` | `F-009` | 전체 | 반려동물 정보 공유 게시판 목록 (카테고리 필터, 검색, 글쓰기 버튼) |
| `P-009` | `/community/:id` | `CommunityDetailView.vue` | `F-009` | 전체 | 게시글 상세, 작성자 수정/삭제, 댓글 목록 및 작성 |
| `P-010` | `/community/new`, `/community/:id/edit` | `CommunityFormView.vue` | `F-009` | `USER` | 게시글 작성/수정 폼 |
| `P-011` | `/health-news` | `HealthNewsListView.vue` | `F-012` | 전체 | 승인된 질병·신약·건강 관리법 정보 목록 (카테고리: 질병/신약/건강관리) 및 상세/원문 이동 |
| `P-012` | `/admin/news` | `AdminNewsReviewView.vue` | `F-010`, `F-011` | `ADMIN` | 크롤링 수동 실행 버튼, 승인 대기(`PENDING`) 기사 검수 테이블, 내용 미리보기·승인/반려 처리 |
| `P-013` | `/ai-food-qa` | `AiFoodQaView.vue` | `F-013` | `USER` | *(Step 7 추후 구현)* 반려동물 선택 후 음식/건강 질문 입력 → 안전·주의·금지 배지 카드 + 이유/급여 팁 + 수의사 상담 면책 문구 표시 |

### 4-2. 핵심 사용자 흐름 (User Flow)

1. **가족 그룹 온보딩**
   - `첫 가입자`: `/signup` → "새 가족 만들기" 선택 → 가입 완료 → **가족 고유 번호(예: `PET-8A3K92`) 발급** → 가족에게 번호 공유
   - `나머지 가족`: `/signup` → "가족 코드로 참여하기" 선택 → 고유 번호 입력 → 가입 즉시 동일 가족 그룹에 합류 → `/` 대시보드에서 가족이 등록한 반려동물과 오늘의 케어 내역 즉시 확인
2. **당일 데일리 케어 공유**
   - `/` 대시보드 접속 → 반려동물 선택 → "아침 식사 완료" 또는 "산책 30분" 클릭 → 즉시 저장되며 다른 가족 화면에도 `완료 (엄마 · 08:15)` 형태로 표시
3. **약 등록 및 투약·잔여 일수 공유**
   - `/pets/:id/medicines/new` 진입 → 약 이름 검색(외부 API/카탈로그 조회) →
     - **검색됨**: 제품명·효능/주의사항 자동 채움 + 사용자가 급여 방법·처방량(예: 1일 2회, 14일치) 입력 후 저장
     - **검색 안 됨**: "직접 등록하기" 선택 → 약 이름·급여 방법·처방량 직접 입력 후 저장
   - `/` 대시보드 또는 `/pets/:id/medicines`에서 오늘 급여 버튼 클릭 → 오늘 투약 완료 표시 + 잔여 일수 자동 갱신(예: `3일치 남음` 강조)
4. **크롤링 정보 관리자 승인 및 공개**
   - 크롤러(스케줄러 또는 관리자 수동 실행)가 질병·신약·건강 관리법 기사 수집 → `PENDING` 저장 → 관리자가 `/admin/news`에서 내용 확인 후 `승인` 클릭 → `/health-news`에 즉시 공개

---

## 5. 데이터 모델 / DB 설계 (Entities 초안)

### 5-1. 엔티티 관계 요약
- `Family` 1 : N `Member` — 한 가족 그룹에 여러 가족 구성원이 소속
- `Family` 1 : N `Pet` — 한 가족 그룹에 여러 반려동물 등록 가능
- `Pet` 1 : N `DailyCareLog` — 반려동물별 날짜별 산책/식사/간식 기록 (`member_id`로 누가 기록했는지 추적)
- `MedicineCatalog` 1 : N `PetMedicine` (Optional FK) — 외부 API/공공데이터 기반 약 정보 참조 (직접 등록 약은 `catalog_id = NULL`)
- `Pet` 1 : N `PetMedicine` — 반려동물별 등록된 약 및 급여 방법·잔여량 관리
- `PetMedicine` 1 : N `MedicationLog` — 날짜·회차별 실제 투약 이력 (`member_id`로 누가 먹였는지 추적)
- `Member` 1 : N `Post`, `Post` 1 : N `Comment` — 정보 공유 커뮤니티 게시판
- `HealthArticle` — 크롤링 수집 및 관리자 승인(`PENDING` / `APPROVED` / `REJECTED`) 콘텐츠 (`reviewed_by` = 관리자 `member_id`)

### 5-2. 테이블 상세

#### 1) `families` (가족 그룹)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 가족 그룹 고유 ID |
| `invite_code` | `String` | NOT NULL, UNIQUE, max 20 | 가족 고유 초대 번호 (예: `PET-8A3K92`) |
| `name` | `String` | max 50 | 가족 별칭 (예: `초코네 가족`, 선택) |
| `created_at` | `LocalDateTime` | NOT NULL | 생성 일시 |

#### 2) `members` (사용자)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 회원 고유 ID |
| `family_id` | `Long` | FK (`families.id`), NULLABLE | 소속 가족 ID (관리자 전용 계정은 NULL 가능) |
| `email` | `String` | NOT NULL, UNIQUE, max 100 | 로그인 이메일 |
| `password` | `String` | NOT NULL | 암호화된 비밀번호 |
| `nickname` | `String` | NOT NULL, max 30 | 가족 내 표시 이름 (예: `엄마`, `희원`) |
| `role` | `MemberRole` (Enum) | NOT NULL | `USER`, `ADMIN` |
| `is_family_owner` | `Boolean` | NOT NULL, default `false` | 최초 가족 생성자 여부 |
| `created_at` | `LocalDateTime` | NOT NULL | 가입 일시 |

#### 3) `pets` (반려동물)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 반려동물 고유 ID |
| `family_id` | `Long` | FK (`families.id`), NOT NULL | 소속 가족 ID |
| `name` | `String` | NOT NULL, max 50 | 이름 (예: `초코`) |
| `species` | `PetSpecies` (Enum) | NOT NULL | `DOG`, `CAT`, `ETC` |
| `breed` | `String` | max 50 | 품종 (예: `말티푸`) |
| `birth_date` | `LocalDate` | NULLABLE | 생년월일 |
| `gender` | `PetGender` (Enum) | NULLABLE | `MALE`, `FEMALE`, `UNKNOWN` |
| `neutered` | `Boolean` | NOT NULL, default `false` | 중성화 여부 |
| `weight_kg` | `BigDecimal` | NULLABLE | 몸무게 (kg) |
| `notes` | `String` | TEXT | 알레르기/특이사항 메모 |
| `created_at` | `LocalDateTime` | NOT NULL | 등록 일시 |

#### 4) `daily_care_logs` (당일 산책·식사·간식 기록)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 기록 고유 ID |
| `pet_id` | `Long` | FK (`pets.id`), NOT NULL | 대상 반려동물 ID |
| `member_id` | `Long` | FK (`members.id`), NOT NULL | 기록한 가족 구성원 ID |
| `care_date` | `LocalDate` | NOT NULL | 기록 날짜 (KST 기준 당일, 조회 인덱스 권장) |
| `care_type` | `CareType` (Enum) | NOT NULL | `WALK`(산책), `MEAL`(식사), `SNACK`(간식) |
| `performed_at` | `LocalDateTime` | NOT NULL | 실제 수행 시각 |
| `memo` | `String` | max 200 | 간단 메모 (예: `공원 30분`, `닭가슴살 1개`) |

#### 5) `medicine_catalog` (동물용 의약품 마스터/API 캐시)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 카탈로그 ID |
| `external_code` | `String` | UNIQUE, max 100 | 공공데이터/외부 API 품목허가번호 등 |
| `name` | `String` | NOT NULL, max 150 | 제품명 |
| `manufacturer` | `String` | max 100 | 제조/수입 업체명 |
| `efficacy` | `String` | TEXT | 효능·효과 |
| `dosage_guide` | `String` | TEXT | 기본 용법·용량 안내 |
| `precautions` | `String` | TEXT | 주의사항 |

#### 6) `pet_medicines` (반려동물별 등록 약)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 등록 약 고유 ID |
| `pet_id` | `Long` | FK (`pets.id`), NOT NULL | 대상 반려동물 ID |
| `catalog_id` | `Long` | FK (`medicine_catalog.id`), NULLABLE | API/카탈로그 연동 시 참조 ID (직접 등록 시 `NULL`) |
| `source_type` | `MedicineSource` (Enum) | NOT NULL | `API`(API 검색 등록), `CUSTOM`(사용자 직접 등록) |
| `name` | `String` | NOT NULL, max 150 | 약 이름 |
| `dosage_method` | `String` | NOT NULL, TEXT | 사용자가 작성한 급여 방법 (예: `아침/저녁 식후 1정, 간식에 숨겨서 급여`) |
| `daily_dose_count` | `Integer` | NOT NULL, default `1` | 1일 급여 횟수 (예: 하루 2회) |
| `total_doses` | `Integer` | NOT NULL | 총 처방 횟수 (예: 14일 × 2회 = 28회분) |
| `remaining_doses` | `Integer` | NOT NULL | 현재 남은 횟수 (투약 체크 시 -1, 취소 시 +1) |
| `start_date` | `LocalDate` | NOT NULL | 복용 시작일 |
| `active` | `Boolean` | NOT NULL, default `true` | 현재 복용 중 여부 |
| `created_by` | `Long` | FK (`members.id`), NOT NULL | 최초 등록한 가족 구성원 |
| `created_at` | `LocalDateTime` | NOT NULL | 등록 일시 |

> **잔여 일수 계산식**: `remainingDays = Math.ceil(remaining_doses / daily_dose_count)` (예: 하루 2회, 5회 남음 → 3일치 남음 / 또는 소수점 `2.5일치` 표기 선택 가능)

#### 7) `medication_logs` (당일 투약 기록)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 투약 기록 ID |
| `pet_medicine_id` | `Long` | FK (`pet_medicines.id`), NOT NULL | 등록 약 ID |
| `member_id` | `Long` | FK (`members.id`), NOT NULL | 투약한 가족 구성원 ID |
| `medicated_date` | `LocalDate` | NOT NULL | 투약 날짜 (KST) |
| `medicated_at` | `LocalDateTime` | NOT NULL | 실제 투약 시각 |
| `memo` | `String` | max 200 | 메모 (예: `잘 삼킴`) |

#### 8) `posts` & `comments` (정보 공유 게시판)
| 테이블 | 주요 컬럼 |
| --- | --- |
| `posts` | `id`, `author_id`(FK `members`), `category`(Enum: `INFO`, `QUESTION`, `REVIEW`, `FREE`), `title`, `content`(TEXT), `view_count`, `created_at`, `updated_at` |
| `comments` | `id`, `post_id`(FK `posts`), `author_id`(FK `members`), `content`(TEXT), `created_at`, `updated_at` |

#### 9) `health_articles` (크롤링 건강·신약 정보)
| 컬럼명 | Java 타입 | 제약조건 | 설명 / 예시 |
| --- | --- | --- | --- |
| `id` | `Long` | PK, Auto Increment | 콘텐츠 고유 ID |
| `category` | `ArticleCategory` (Enum) | NOT NULL | `DISEASE`(질병), `NEW_DRUG`(신약), `HEALTH_CARE`(건강관리) |
| `title` | `String` | NOT NULL, max 255 | 기사/정보 제목 |
| `summary` | `String` | TEXT | 요약 내용 (크롤링 추출 또는 관리자 편집) |
| `source_name` | `String` | max 100 | 출처 매체/기관명 |
| `source_url` | `String` | NOT NULL, UNIQUE, max 500 | 원문 URL (중복 수집 방지 기준) |
| `published_at` | `LocalDateTime` | NULLABLE | 원문 발행일 |
| `status` | `ArticleStatus` (Enum) | NOT NULL, default `PENDING` | `PENDING`(승인대기), `APPROVED`(승인), `REJECTED`(반려) |
| `reviewed_by` | `Long` | FK (`members.id`), NULLABLE | 검수한 관리자 ID |
| `reviewed_at` | `LocalDateTime` | NULLABLE | 승인/반려 일시 |
| `crawled_at` | `LocalDateTime` | NOT NULL | 크롤링 수집 일시 |

---

## 6. API 명세 (`petCare_serv` 초안)

### 6-1. API 공통 규칙
- **Base Path**: `/api/v1`
- **시간대**: 모든 날짜(`careDate`, `medicatedDate`)는 `Asia/Seoul` (KST) 기준 `YYYY-MM-DD`
- **에러 응답 형식(권장)**:
  ```json
  {
    "code": "INVALID_INVITE_CODE",
    "message": "유효하지 않은 가족 고유 번호입니다."
  }
  ```

### 6-2. 엔드포인트 목록

| API ID | 관련 기능 | Method | Path | 설명 |
| --- | --- | --- | --- | --- |
| `API-001` | `F-001`, `F-002` | `POST` | `/api/v1/auth/signup` | 회원가입 (신규 가족 생성 또는 초대 코드 입력 합류) |
| `API-002` | `F-003` | `POST` | `/api/v1/auth/login` | 로그인 |
| `API-003` | `F-003` | `GET` | `/api/v1/families/me` | 내 가족 정보, 초대 코드, 가족 구성원 목록 조회 |
| `API-004` | `F-004` | `GET` | `/api/v1/pets` | 우리 가족 반려동물 목록 조회 |
| `API-005` | `F-004` | `POST` | `/api/v1/pets` | 반려동물 등록 |
| `API-006` | `F-004` | `PUT` / `DELETE` | `/api/v1/pets/{petId}` | 반려동물 수정 / 삭제 |
| `API-007` | `F-005` | `GET` | `/api/v1/pets/{petId}/care-logs?date=YYYY-MM-DD` | 특정 날짜(기본 오늘)의 산책/식사/간식 기록 조회 |
| `API-008` | `F-005` | `POST` | `/api/v1/pets/{petId}/care-logs` | 산책/식사/간식 기록 추가 |
| `API-009` | `F-005` | `DELETE` | `/api/v1/care-logs/{logId}` | 잘못 누른 케어 기록 취소(삭제) |
| `API-010` | `F-006` | `GET` | `/api/v1/medicines/search?keyword=...` | 외부 API / 의약품 카탈로그 검색 |
| `API-011` | `F-006`, `F-007` | `GET` / `POST` | `/api/v1/pets/{petId}/medicines` | 반려동물 약 목록(오늘 투약 여부+잔여 일수 포함) 조회 / 약 등록 |
| `API-012` | `F-007` | `PUT` / `DELETE` | `/api/v1/pets/{petId}/medicines/{medicineId}` | 약 정보·급여 방법·잔여량 수정 / 삭제 |
| `API-013` | `F-008` | `POST` | `/api/v1/pets/{petId}/medicines/{medicineId}/logs` | 오늘 투약 완료 체크 (잔여량 자동 차감) |
| `API-014` | `F-008` | `DELETE` | `/api/v1/medication-logs/{logId}` | 투약 체크 취소 (잔여량 자동 복구) |
| `API-015` | `F-009` | `GET` / `POST` | `/api/v1/posts` | 커뮤니티 게시글 목록(검색/카테고리/페이징) / 작성 |
| `API-016` | `F-009` | `GET` / `PUT` / `DELETE` | `/api/v1/posts/{postId}` | 게시글 상세 / 수정 / 삭제 |
| `API-017` | `F-009` | `POST` / `DELETE` | `/api/v1/posts/{postId}/comments` | 댓글 작성 / 삭제 |
| `API-018` | `F-012` | `GET` | `/api/v1/health-articles` | 승인(`APPROVED`)된 질병·신약·건강 정보 목록 조회 |
| `API-019` | `F-010` | `POST` | `/api/v1/admin/health-articles/crawl` | (관리자) 크롤링 수동 실행 |
| `API-020` | `F-011` | `GET` | `/api/v1/admin/health-articles?status=PENDING` | (관리자) 승인 대기/전체 수집 목록 조회 |
| `API-021` | `F-011` | `PATCH` | `/api/v1/admin/health-articles/{articleId}/review` | (관리자) 승인(`APPROVED`) / 반려(`REJECTED`) 및 요약 수정 |
| `API-022` | `F-013` | `POST` | `/api/v1/ai/food-qa` | *(Step 7 추후 구현)* 반려동물 ID + 질문 전달 → 외부 LLM 중계 호출 후 구조화 답변(`SAFE/CAUTION/DANGER`, 이유, 팁) 반환 |
| `API-023` | `F-014` | `GET` | `/api/v1/families/me/events` | SSE 실시간 이벤트 구독 (`text/event-stream`, 가족 내 케어/투약 변경 이벤트 푸시) |

---

## 7. 비즈니스 규칙 & 예외 처리 (Business Rules)

1. **가족 데이터 격리 (보안 필수)**
   - 반려동물, 데일리 케어 기록, 약/투약 기록의 모든 조회·수정·삭제 API는 **요청자의 `familyId`와 대상 데이터의 `familyId` 일치 여부**를 반드시 검증한다.
2. **가족 고유 번호 규칙**
   - 헷갈리는 문자(`0/O`, `1/I/l`)를 제외한 대문자+숫자 8자리(예: `PET-7K4M9Q`)로 자동 생성하며 DB `UNIQUE` 제약으로 중복 방지한다.
3. **당일 케어 공유 기준**
   - 날짜 기준은 서버 `Asia/Seoul`(KST) 00:00 ~ 23:59로 한다.
   - 하루에 식사/산책/간식은 여러 번 일어날 수 있으므로 단순 Boolean 체크가 아니라 **수행 이력 리스트(시간 + 기록한 가족 닉네임 + 메모)**로 저장하고, 대시보드에는 오늘 수행 횟수 및 완료 여부를 요약 표시한다.
4. **동물 의약품 API 연동 & Fallback 정책**
   - 공공데이터포털의 **농림축산검역본부_동물용의약품 품목허가 현황**은 주로 CSV 파일 데이터 형태로 제공되므로, 초기 적재(배치/CSV Import)로 `medicine_catalog` DB를 구성하거나 Open API 연동 후 로컬 캐싱하는 방식이 안정적이다.
   - 검색 결과가 없거나 사용자가 원하는 약이 없을 경우, `source_type = CUSTOM`으로 즉시 수동 등록할 수 있어야 한다.
5. **투약 잔여량 & 잔여 일수 계산**
   - 투약 체크(`POST /logs`) 시 `remaining_doses`를 `1` 차감하고(최소 `0`), 체크 취소(`DELETE /logs/{id}`) 시 `1` 복구한다.
   - 오늘 이미 `daily_dose_count`만큼 모두 투약했다면 프론트에서 "오늘 급여 완료" 상태를 표시하고, 추가 클릭 시 중복 급여 경고 확인창을 띄운다.
   - 잔여 일수가 **3일 이하**이면 `소진 임박` 배지를, `0`이면 `복용 완료/소진` 상태를 표시한다.
6. **크롤링 저작권 및 중복 방지 정책**
   - 외부 기사 본문 전체를 무단 복제하지 않고 **제목, 짧은 요약, 출처명, 원문 바로가기 링크(`source_url`)** 중심으로 노출하는 것을 권장한다.
   - `source_url`을 `UNIQUE` 키로 두어 이미 수집된 기사는 중복 저장하지 않는다.
   - 반드시 `PENDING` 상태로 먼저 저장되며, **관리자(`ADMIN`) 승인 전에는 일반 사용자 API에 절대 노출되지 않는다.**
7. **AI 음식·건강 Q&A 안전성 및 비용 정책 (Step 7)**
   - LLM API 키는 반드시 백엔드(`petCare_serv`) 환경변수로만 관리하고, 프론트에서 직접 호출하지 않는다.
   - 요청 시 선택한 반려동물의 `species`(개/고양이), 나이, 몸무게, `notes`(알레르기/특이사항)를 시스템 프롬프트에 자동 포함시킨다.
   - 응답은 `verdict`(`SAFE` / `CAUTION` / `DANGER`), `reason`, `tip` JSON 구조로 받아 모바일 배지 카드로 표시하고, 하단에 *"AI 참고 정보이며 실제 급여·응급 상황 시 수의사와 상담하세요."* 면책 문구를 고정 노출한다.
   - 비용/남용 방지를 위해 사용자별 일일 질문 횟수 제한과 자주 묻는 음식(종 + 음식명) 결과 캐싱을 적용한다.
8. **Redis 활용 및 관측·로그 정책**
   - **캐싱**: 당일 케어 대시보드(`family:{familyId}:care:{date}`), 의약품 검색, AI Q&A 결과를 Redis에 캐싱하고 쓰기 발생 시 즉시 무효화한다.
   - **동시성 제어**: 투약 체크 API에 짧은 Redis 분산 락(3~5초)을 적용해 가족 간 동시 클릭으로 인한 잔여량 중복 차감을 방지한다.
   - **실시간 동기화**: 케어/투약 변경 시 Redis Pub/Sub 채널(`family:events:{familyId}`)로 발행 → 연결된 모든 `SseEmitter` 클라이언트에 푸시한다.
   - **로그·메트릭**: 모든 요청에 MDC `traceId`, `familyId`, `memberId`를 주입해 JSON 로그로 출력하고, Prometheus/Loki/Grafana(PLG)로 API 지연·에러율·크롤러 실패·Redis 히트율을 추적한다.

---

## 8. UI/UX & 디자인 가이드 (`petCare_web`)

### 8-1. 모바일 세로형 앱 디자인 (에브리타임 스타일 App Shell)
> 추후 **PWA** 또는 **Capacitor(WebView)** 등으로 간편하게 모바일 앱으로 전환할 수 있도록, 처음부터 스마트폰 세로 화면에 최적화된 앱 UI 구조로 개발합니다.

- **세로형 고정 프레임 (Mobile Container)**
  - 메인 컨테이너 폭을 **`max-w-[430px]`(또는 `max-w-md`) 중앙 정렬 + `min-h-dvh`**로 고정
  - PC 브라우저에서 접속해도 화면이 옆으로 퍼지지 않고, 바깥 여백(`bg-slate-100`) 위에 스마트폰 앱 화면처럼 깔끔한 세로 프레임(`bg-white`, 얇은 경계선/그림자)으로 표시
  - *(예외)* 관리자 검수 페이지(`P-012` `/admin/news`)는 작업 편의상 필요 시 PC 와이드 레이아웃 선택 가능
- **앱 고정 레이아웃 (App Shell)**
  - **상단 앱바 (Top App Bar, 높이 `56px` 고정)**: 좌측 로고 또는 뒤로가기(`←`) + 현재 화면 타이틀, 우측 보조 액션(가족 코드 복사 / 설정 등)
  - **본문 세로 피드 (Scrollable Body)**: 에브리타임처럼 위에서 아래로 자연스럽게 스크롤되는 카드·리스트 기반 세로형 구성
  - **하단 탭바 (Bottom Navigation Bar, 높이 `60px` 고정 + Safe Area)**:
    1. `홈` (`/`) — 오늘의 산책·식사·간식·투약 체크 대시보드
    2. `반려동물·약` (`/pets`) — 반려동물 관리 & 복용 약·잔여 일수
    3. `커뮤니티` (`/community`) — 정보 공유 게시판
    4. `건강정보` (`/health-news`) — 관리자 승인 질병·신약·건강 관리 피드
    5. `내 가족` (`/family`) — 가족 초대 번호·구성원·설정
- **모바일 앱 전환(PWA / Capacitor) 대비 UI 규칙**
  - **터치 영역**: 버튼·체크박스 등 인터랙션 요소는 최소 `44px × 44px` 이상 확보
  - **모바일 뷰포트 대응**: `100vh` 대신 `100dvh` (`min-h-dvh`) 사용, 노치·하단 홈바 대응 `viewport-fit=cover` 및 `safe-area-inset` 패딩 적용
  - **인터랙션**: 마우스 `hover`에 의존하지 않고 터치 피드백(`active:scale-95` 등) 중심 설계, 기본 브라우저 `alert/confirm` 대신 **바텀시트(Bottom Sheet)**·토스트 메시지·커스텀 모달 사용
  - **API 호출 구조**: 앱(WebView) 패키징 시에도 그대로 동작하도록 `VITE_API_BASE_URL` 환경변수 기반으로 백엔드 호출 경로를 분리

### 8-2. 비주얼 톤앤매너
- **디자인 컨셉**: 가족 모두가(부모님 세대 포함) 한 손으로 쉽게 누르고 확인할 수 있는 크고 명확한 카드형 UI, 따뜻하고 신뢰감 있는 분위기
- **메인 컬러 (Tailwind 기준)**: 메인 `emerald-600`, 경고/약 소진 임박 `amber-500` / `rose-500`, 앱 배경 `slate-50`, PC 바깥 배경 `slate-100`

---

## 9. 개발 마일스톤 (구현 순서 추천)

- [x] **Step 1 (인프라 기반 & 모바일 앱 셸 + 가족/반려동물)**: `docker-compose.yml`(MariaDB + Redis) 구성 + JSON Logback(MDC)/Swagger 기본 설정 + `petCare_web` 세로형 모바일 App Shell(상단 앱바·하단 5탭) + 가족 생성/초대코드 합류 + 반려동물 CRUD
- [x] **Step 2 (핵심 케어 공유 + Redis 캐싱 & SSE)**: 당일 산책·식사·간식 기록 + Redis 캐싱 + **SSE(+ Redis Pub/Sub) 실시간 가족 동기화** + 세로 피드형 대시보드 화면 구현
- [x] **Step 3 (약 & 투약 관리)**: 공공데이터 CSV 기반 의약품 카탈로그 검색/직접 등록 + 급여 방법 + 오늘 투약 체크(Redis 동시성 락) & 잔여 일수 계산 구현
- [x] **Step 4 (커뮤니티)**: 반려동물 정보 공유 게시판 (글/댓글/검색 + Redis 조회수 중복 방지/동기화) 구현
- [ ] **Step 5 (크롤링 & 관리자)**: `@Scheduled` + ShedLock 질병·신약·건강 정보 크롤러 + 관리자 검수/승인 화면 + 사용자 열람 페이지 구현
- [ ] **Step 6 (인증/보안 + 모니터링 + 앱 전환 준비)**: Spring Security JWT(+ Redis Refresh Token) 인증 적용 + `ADMIN` 권한 분리 + PLG 모니터링 스택(`--profile monitoring`: Prometheus + Grafana + Loki) 대시보드 구성 + PWA/Capacitor 앱 패키징 및 배포 준비
- [ ] **Step 7 (확장 기능 — AI 음식·건강 Q&A)**: 외부 LLM API 연동(`petCare_serv` 중계) + 반려동물 정보 반영 프롬프트 + 구조화 응답(`안전/주의/금지` 카드) + Redis 일일 횟수 제한/캐싱 구현

---

## 10. 미정 사항 / 함께 결정할 것 (Open Questions)

- [ ] **인증 방식**: Step 6 로그인 도입 시 `JWT (+ Redis Refresh Token)` 방식을 확정할지 확인 (초기 Step 1~5 개발은 인증 없이 테스트용 가족/회원 기반으로 빠르게 구현 가능)
- [ ] **동물 의약품 데이터 방식**: 공공데이터포털의 `농림축산검역본부 동물용의약품 품목허가 현황(CSV)`을 내려받아 `medicine_catalog` 테이블에 적재해 검색 API로 제공할지 확인
- [ ] **크롤링 대상 사이트**: 질병·신약·건강 관리법 정보를 어떤 출처(예: 수의학/반려동물 뉴스 RSS, 농림축산검역본부 공지, 특정 포털 뉴스 검색 API 등)에서 수집할지 결정
- [ ] **반려동물 프로필 사진**: 반려동물 사진이나 게시판 이미지 업로드 기능이 1차(MVP)에 포함될지, 추후 추가할지 결정
- [ ] **외부 LLM 선택 (Step 7)**: 추후 AI 음식 Q&A 구현 시 `Google Gemini API` vs `OpenAI API` 중 선택

---

## 11. 구현 현황 & 이어서 작업하기 (Handoff Log)

> **언제 어디서든(새 PC · 새 세션) 이 섹션만 읽고 바로 이어서 작업할 수 있도록 최신 구현 상태와 컨벤션을 기록합니다.**
>
> - **최종 업데이트**: `2026-11-23`
> - **완료 단계**: **Step 1~4 + PrimeVue UI 통합 마이그레이션 완료** (인프라·앱셸·가족·반려동물·케어·SSE·약/투약·커뮤니티)
> - **다음 시작 단계**: **Step 5 (질병·신약·건강 정보 크롤러 + 관리자 검수/승인 + 건강정보 피드)**

### 11-1. 구현 완료 현황 요약

#### 1) 루트 인프라 & DB 마이그레이션
| 파일 | 역할 |
| --- | --- |
| `docker-compose.yml`, `.env.example` | MariaDB 11.4(`utf8mb4`, `Asia/Seoul`, DB `petcare`) + Redis 7 Alpine 컨테이너 구성 |
| `petCare_serv/.../db/migration/V1__init_family_and_pets.sql` | `families`, `members`, `pets` 테이블 및 인덱스 |
| `petCare_serv/.../db/migration/V2__create_daily_care_logs.sql` | `daily_care_logs` 테이블 및 `(pet_id, care_date, performed_at)` 복합 인덱스 |
| `petCare_serv/.../db/migration/V3__create_medicine_tables.sql` | `medicine_catalog`, `pet_medicines`, `medication_logs` 테이블 및 인덱스 |
| `petCare_serv/.../db/migration/V4__create_community_tables.sql` | `posts`, `comments` 테이블 및 카테고리/최신순 복합 인덱스 |

#### 2) 백엔드 (`petCare_serv`, 패키지 루트 `com.petCare.petCare`)
| 영역 | 주요 파일 / 클래스 | 구현된 엔드포인트 & 기능 |
| --- | --- | --- |
| 공통 설정 | `config/SecurityConfig`, `JpaAuditingConfig`, `OpenApiConfig`, `SchedulingConfig`, `logback-spring.xml` | CORS 환경변수화, `BCryptPasswordEncoder`, Swagger(`/swagger-ui.html`), MDC(`traceId`, `memberId`) 로깅 |
| 예외/인증 추상화 | `global/exception/*`, `global/auth/CurrentMemberService`, `global/logging/MdcLoggingFilter` | 통일된 `ErrorResponse`, `X-Member-Id` 헤더 기반 회원/가족 검증 |
| 인증 & 가족 | `domain/auth/*`, `domain/family/*`, `domain/member/*` | `POST /api/v1/auth/signup` (`CREATE_FAMILY` / `JOIN_FAMILY`), `POST /api/v1/auth/login`, `GET /api/v1/families/me` |
| 반려동물 | `domain/pet/*` (`FamilyPetService`, `FamilyPetController`) | `GET/POST /api/v1/pets`, `GET/PUT/DELETE /api/v1/pets/{petId}` (소프트 삭제, 가족 격리) |
| 당일 케어 + 캐시 | `domain/care/*` (`DailyCareService`, `DailyCareController`, `RedisDailyCareCacheStore`, `InMemoryDailyCareCacheStore`) | `GET/POST /api/v1/pets/{petId}/care-logs`, `DELETE /api/v1/care-logs/{logId}` (KST 기준, Redis 캐시 TTL 10분 + 쓰기 시 무효화) |
| 실시간 SSE/PubSub | `domain/realtime/*` (`FamilyEventController`, `FamilySseRegistry`, `RedisFamilyEventBroker`, `InMemoryFamilyEventBroker`, `RedisPubSubConfig`) | `GET /api/v1/families/me/events` (`text/event-stream`, 25초 하트비트, Redis Pub/Sub `petcare:events:family:{familyId}` 브로드캐스트) |
| 약 & 투약 관리 | `domain/medicine/*` (`MedicineController`, `MedicineCatalogService`, `MedicineCatalogSeeder`, `PetMedicineService`, `RedisMedicationLockManager`, `InMemoryMedicationLockManager`, `RedisMedicineSearchCacheStore`) | `GET /api/v1/medicines/search`, `GET/POST/PUT/DELETE /api/v1/pets/{petId}/medicines`, `POST .../logs`, `DELETE /api/v1/medication-logs/{logId}` (동시 클릭 5초 락 + 잔여량/잔여일수 자동 계산 + SSE) |
| 커뮤니티 게시판 | `domain/community/*` (`CommunityController`, `CommunityService`, `RedisPostViewTracker`, `InMemoryPostViewTracker`, `PostViewSyncScheduler`) | `GET/POST /api/v1/posts`, `GET/PUT/DELETE /api/v1/posts/{postId}`, `POST/DELETE /api/v1/posts/{postId}/comments` (24시간 중복 방지 조회수 + 주기적 DB 반영) |
| 통합 테스트 | `PetCareApplicationTests`, `Step1FamilyAndPetIntegrationTest`, `Step2CareAndRealtimeIntegrationTest`, `Step3MedicineIntegrationTest`, `Step4CommunityIntegrationTest` | H2(`test` 프로필) 기반 Step 1~4 전체 플로우, 캐시 무효화, 동시성 락 충돌(409), 작성자/가족 권한 검증, 조회수 중복 방지 검증 |

#### 3) 프론트엔드 (`petCare_web`, Vue 3 + JavaScript + Tailwind CSS v4 + PrimeVue)
| 영역 | 주요 파일 | 구현 내용 |
| --- | --- | --- |
| 환경 & API | `.env`, `.env.example`, `src/api/client.js`, `auth.js`, `pets.js`, `care.js`, `medicines.js`, `posts.js` | `VITE_API_BASE_URL` 기반 fetch 래퍼, `X-Member-Id` 자동 첨부, `EventSource` SSE 스트림, 의약품·커뮤니티 API |
| 테마 & 유틸 | `src/main.js`, `src/assets/main.css`, `src/stores/auth.js`, `src/stores/toast.js`, `src/utils/petFormat.js` | PrimeVue `Aura` 프리셋(`cssLayer` + `darkModeSelector: '.app-dark'`), `ToastService` 래퍼, `DatePicker` ISO 날짜 변환, 세션 유지 |
| 모바일 App Shell | `index.html`, `src/App.vue`, `AppTopBar.vue`, `AppBottomNav.vue`, `ConfirmSheet.vue`, `CareLogSheet.vue`, `MedicineFormSheet.vue` | `max-w-[430px]` 세로형 고정 프레임, `min-h-dvh`, Safe Area 대응, PrimeVue `Toast`·`Drawer(position="bottom")`·`PrimeIcons` |
| 화면 & 라우터 | `src/router/index.js`, `HomeView`, `LoginView`, `SignupView`, `PetListView`, `PetFormView`, `FamilyView`, `CommunityView`, `PostDetailView`, `PostFormView`, `HealthNewsView`(준비중) | 전 화면 PrimeVue(`InputText`, `Password`, `DatePicker`, `InputNumber`, `SelectButton`, `ToggleSwitch`, `Tag`, `Message`, `Button` 등) 통일 |

### 11-2. 이어서 개발 시 꼭 지킬 기술 컨벤션
1. **Spring Boot 4.1.2 호환성**
   - JSON 직렬화는 Jackson 3(`tools.jackson.databind.json.JsonMapper`) 사용 (`com.fasterxml.jackson.*` 직접 import 지양).
   - Flyway는 `spring-boot-starter-flyway` + `flyway-mysql` 유지, Swagger는 `springdoc-openapi-starter-webmvc-ui:3.2.1` 유지.
2. **가족 데이터 격리 & 인증 교체 포인트**
   - 모든 가족 소속 도메인 서비스는 `CurrentMemberService.requireMemberWithFamily(memberId)`로 소속 가족을 검증하고, 타 가족 리소스 접근 시 `404`로 격리한다.
   - Step 6 JWT 도입 시 `CurrentMemberService`와 프론트 `src/api/client.js`만 교체하면 전체가 전환되도록 유지한다.
3. **Docker 없는 테스트 환경 호환 (`app.redis.enabled`)**
   - Redis에 의존하는 기능(캐시, Pub/Sub, Step 3 분산 락 등)은 인터페이스 + `@ConditionalOnProperty(name = "app.redis.enabled", ...)`로 Redis 구현체와 테스트용 InMemory 구현체를 분리한다.
   - `application-test.properties`는 H2(`MODE=MariaDB`), `flyway.enabled=false`, `app.redis.enabled=false`, `app.scheduling.enabled=false`를 사용한다.
4. **트랜잭션 & SSE 발행 순서**
   - DB 변경 후 SSE 이벤트 발행은 `TransactionSynchronizationManager.afterCommit()`에서 실행해 롤백 시 오발송 및 커밋 전 조회 레이스를 방지한다.
5. **모바일 UI & PrimeVue 원칙 (`petCare_web`)**
   - JavaScript 기반 유지(TypeScript 미사용), 터치 영역 최소 `44px`, 브라우저 기본 `alert()`/`confirm()` 대신 PrimeVue `Toast`·`Drawer(position="bottom")` 기반 바텀시트(`ConfirmSheet` 등) 사용.
   - Tailwind v4 + PrimeVue `Aura` 충돌 방지를 위해 `cssLayer` (`theme, base, primevue, utilities`)와 `darkModeSelector: '.app-dark'`를 유지하고, `DatePicker`는 `parseIsoDate`/`formatIsoDate`로 `YYYY-MM-DD`와 상호 변환한다.
6. **도메인형 패키지 구조 & 환경변수 기반 DB/배포 전환**
   - 백엔드는 계층형이 아닌 **도메인형(`domain/{auth,family,member,pet,care,realtime,medicine,community,...}`)** 구조를 유지한다.
   - 실제 실행(`src/main`)은 MariaDB + Flyway(`V1`~`V4`) 기준이며, Docker MariaDB·로컬 MariaDB·향후 AWS RDS 모두 코드 수정 없이 환경변수(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`)로 전환한다.
   - 모든 컨트롤러는 `@Tag` / `@Operation` (`io.swagger.v3.oas.annotations.*`)으로 문서화되어 서버 실행 후 `/swagger-ui.html`에서 즉시 테스트할 수 있다.

### 11-3. 검증 & 로컬 실행 명령어 (Windows PowerShell)

#### Docker 없이 코드 검증 (현재 환경에서도 즉시 실행 가능)
```powershell
# 1. 백엔드 전체 테스트 (H2 + InMemory 캐시/SSE)
.\petCare_serv\gradlew.bat -p petCare_serv test

# 2. 프론트엔드 프로덕션 빌드 검증
npm --prefix petCare_web run build
```

#### Docker / MariaDB 없이 즉시 체험 실행 (`local-h2` 프로필, 재시작 시 데이터 초기화)
```powershell
# 1. 백엔드 실행 (H2 인메모리 DB + InMemory 캐시/락/SSE)
.\petCare_serv\gradlew.bat -p petCare_serv bootRun --args='--spring.profiles.active=local-h2'

# 2. 프론트엔드 실행 (http://localhost:5173)
npm --prefix petCare_web run dev
```

#### Docker Desktop 설치 후 전체 로컬 실행 (MariaDB + Redis + Backend + Frontend)
```powershell
# 1. MariaDB + Redis 컨테이너 기동 (프로젝트 루트)
docker compose up -d

# 2. 백엔드 실행 (http://localhost:8080, Swagger: http://localhost:8080/swagger-ui.html)
.\petCare_serv\gradlew.bat -p petCare_serv bootRun

# 3. 프론트엔드 실행 (http://localhost:5173)
npm --prefix petCare_web run dev
```

### 11-4. 다음 작업 시작 가이드 — Step 5 (크롤링 & 관리자 검수 · 건강정보 피드)
다음 세션에서 바로 착수할 체크리스트:
1. **DB 마이그레이션**: `V5__create_health_articles.sql` 작성 (`health_articles` — `source_url UNIQUE`, `(status, category, published_at DESC)` 인덱스)
2. **백엔드 (`domain/healthnews`)**:
   - Jsoup/RSS 기반 반려동물 질병·신약·건강 정보 크롤러 + 중복 URL 스킵 + `PENDING` 상태 저장 + `@Scheduled` 스케줄러(분산 락 추상화 적용)
   - 관리자 API(`ADMIN` 권한 검증): 수동 크롤링 실행(`POST /api/v1/admin/health-articles/crawl`), 대기/전체 목록(`GET /api/v1/admin/health-articles`), 승인/반려 및 요약 수정(`PATCH /api/v1/admin/health-articles/{articleId}/review`)
   - 일반 사용자 공개 API: `APPROVED` 기사만 노출(`GET /api/v1/health-articles?category=&keyword=&page=`)
   - `Step5HealthNewsIntegrationTest` 작성 및 `gradlew test` 검증
3. **프론트엔드 (`petCare_web`)**:
   - `src/api/healthNews.js` 추가
   - `HealthNewsView.vue` (`/health-news`): 승인된 질병·신약·건강관리 피드 + 카테고리 필터 + 원문 링크
   - `AdminHealthNewsView.vue` (`/admin/news`): 관리자용 수동 수집 버튼, `PENDING` 기사 요약 수정·승인·반려 화면 구현 및 `npm run build` 검증

### 11-5. 남은 로드맵 요약 (Step 5 → Step 7)
- **Step 5**: 크롤링 & 관리자 검수 · 승인된 건강정보 피드
- **Step 6**: Spring Security JWT(+ Redis Refresh Token) 적용, `ADMIN` 권한 분리, PLG 모니터링(`--profile monitoring`), 배포용 `Dockerfile`/Docker Compose & PWA/Capacitor 앱 전환 준비
- **Step 7**: 외부 LLM API 연동 AI 음식·건강 Q&A (`SAFE`/`CAUTION`/`DANGER` 카드 + Redis 일일 횟수 제한/캐싱)
