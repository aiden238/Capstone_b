# Team Blackbox — 개발 우선순위 & TODO

## 현재 Phase: MVP (1~8주차)

---

## Phase 1: 인프라 & 기반 구축 ✅ 완료 (2026-03-23)

### 🔴 P0 — Docker Compose 인프라 셋업 ✅
- [x] 프로젝트 루트 `docker-compose.yml` 작성 ✅ (2026-03-23)
- [x] PostgreSQL 16 컨테이너 설정 (`db` 서비스) ✅
- [x] `pgdata` 볼륨 (DB 영속), `uploads` 볼륨 (파일 저장) ✅
- [x] Spring Boot Dockerfile 작성 (multi-stage 빌드) ✅
- [x] Next.js Dockerfile 작성 (standalone 빌드) ✅
- [x] Nginx 설정 (`/` → frontend, `/api` → backend, `/uploads` → 정적) ✅
- [x] `docker compose up -d` 로 전체 스택 기동 확인 ✅ (5컨테이너: db, redis, backend, frontend, nginx)
- [x] `.env` 환경변수 파일 구성 (DB 비밀번호, JWT 시크릿 등) ✅
- **참조:** `docker.md`

### 🔴 P0 — 프로젝트 초기화 ✅
- [x] Spring Boot 프로젝트 생성 (Java 25, Gradle 9.4.1 Groovy DSL) ✅ (2026-03-23)
- [x] Next.js 프로젝트 생성 (App Router, TypeScript, Tailwind) ✅
- [ ] GitHub 리포지토리 생성 & 브랜치 전략 확정 (GitHub Flow 선택 — select.md 참조)
- [ ] CI/CD 기본 설정 (GitHub Actions — Docker 빌드 확인)
- [x] **개발 도구 설치 완료** ✅ (2026-03-23)
  - Java 25 (Temurin 25.0.2+10) ✅
  - Node.js 25.8.1 + npm 11.11.0 ✅
  - Docker Desktop 4.65.0 ✅
  - Gradle 9.4.1 (wrapper) ✅
- [x] `gradle wrapper` + `bootJar` 빌드 성공 ✅ (Lombok 1.18.44로 Java 25 호환)
- [x] `cd frontend && npm install` 성공 ✅ (Next.js 14.2.35 보안 패치 적용)

### 🔴 P0 — DB 스키마 v1 배포 (Flyway) ✅
- [x] Flyway 의존성 추가 & 설정 (build.gradle + application.yml) ✅ (2026-03-23)
- [x] `V1__init_users.sql` — users 테이블 ✅
- [x] `V2__init_projects.sql` — projects, project_members 테이블 ✅
- [x] `V3__init_tasks.sql` — tasks, task_assignees 테이블 ✅
- [x] `V4__init_meetings.sql` — meetings, meeting_attendees 테이블 ✅
- [x] `V5__init_activity_logs.sql` — activity_logs 테이블 + 인덱스 ✅
- [x] `V6__init_file_vault.sql` — file_vault + immutable 트리거 + tamper_detection_log ✅
- [x] `V7__init_scores_alerts.sql` — contribution_scores, alerts, weight_configs, weight_change_log ✅
- [x] `docker compose up` 시 Flyway 자동 마이그레이션 확인 ✅ (7개 모두 적용, 15 테이블)
- **참조:** `docker.md`

---

## Phase 2: 인증 & 프로젝트 관리 ✅ 완료 (2026-03-30)

### 🔴 P0 — 백엔드: 인증 ✅
- [x] User 엔티티 & Repository ✅
- [x] 회원가입 API (`POST /api/auth/signup`) ✅
- [x] 로그인 API (`POST /api/auth/login`) — JWT 발급 ✅
- [x] JWT 필터 & SecurityConfig ✅
- [x] Refresh Token 로직 (Redis 기반) ✅
- [x] 역할 기반 접근 제어 (STUDENT / PROFESSOR / TA) ✅
- [x] Docker 빌드 성공, signup API 201 응답 확인 ✅
- [x] 인증 없이 접근 시 401 반환 (JwtAuthenticationEntryPoint) ✅ (2026-03-30)
- [x] Logout 시 access token Redis 블랙리스트 등록 ✅ (2026-03-30)
- **참조:** `backend/modules/auth.md`

### 🔴 P0 — 백엔드: 프로젝트 관리 ✅
- [x] Project CRUD API ✅
- [x] 초대 코드 생성 & 참여 API ✅
- [x] 멤버 관리 (역할 변경, 탈퇴) ✅
- [x] ProjectAccessChecker (LEADER/MEMBER/OBSERVER 권한 검증) ✅
- [x] 데이터 수집 동의 기록 API ✅
- **참조:** `backend/modules/project.md`

### ✅ Phase 2 API 검증 완료 (2026-03-30)
> 전체 17개 API 테스트 통과 (`api-test.mjs`):
> - `POST /api/auth/signup` ✅ 201
> - `POST /api/auth/login` ✅ 200 (accessToken + refreshToken)
> - `GET /api/auth/me` ✅ 200
> - `POST /api/auth/refresh` ✅ 200
> - `GET /api/auth/me` (no token) ✅ 401
> - `POST /api/projects` ✅ 201
> - `GET /api/projects` ✅ 200
> - `GET /api/projects/:id` ✅ 200
> - `POST /api/projects/join` ✅ 201
> - `GET /api/projects/:id/members` ✅ 200
> - `PATCH /api/projects/:id/members/:id/role` ✅ 200
> - `PATCH /api/projects/:id/members/me/consent` ✅ 200
> - `POST /api/projects/:id/invite-code` ✅ 200
> - `POST /api/auth/logout` ✅ 200
> - `GET /api/auth/me` (logout 후) ✅ 401 (블랙리스트 작동)

### 🟡 P1 — 프론트엔드: 인증 & 프로젝트 ✅ (2026-03-30)
- [x] 로그인/회원가입 페이지 ✅
- [x] JWT 토큰 관리 (Zustand store) ✅
- [x] Axios 인터셉터 (토큰 자동 첨부, 갱신) ✅
- [x] 프로젝트 목록 페이지 ✅
- [x] 프로젝트 생성 모달 ✅
- [x] 초대 코드 참여 페이지 ✅
- [x] 동의 플로우 온보딩 UI ✅ (2026-04-06) — 프로젝트 진입 시 동의 모달 표시
- **참조:** `frontend/pages/auth.md`, `frontend/pages/dashboard.md`

---

## Phase 3: 칸반 & 회의록 (5~6주) — ✅ 완료 (2026-03-30)

### 🔴 P0 — 백엔드: 태스크 ✅ (2026-03-30)
- [x] Task CRUD API (생성/수정/삭제/목록) ✅
- [x] 상태 변경 API (TODO → IN_PROGRESS → DONE) ✅
- [x] 담당자 배정 API (추가/제거) ✅
- [x] 태스크 이벤트 → `activity_logs` 자동 기록 ✅ (TASK_CREATE/UPDATE/COMPLETE/DELETE/ASSIGN/UNASSIGN)
- [x] ActivityLog 엔티티 & 서비스 공통 모듈 생성 ✅
- **참조:** `backend/modules/task.md`

### 🔴 P0 — 백엔드: 회의록 ✅ (2026-03-30)
- [x] Meeting CRUD API ✅
- [x] 체크인 코드 생성 & 체크인 API ✅ (SecureRandom 8자리 코드, 중복 체크인 방지)
- [x] 회의록 작성/수정 API ✅ (notes, decisions 필드)
- [x] 액션 아이템 → 태스크 생성 연결 ✅ (2026-04-06) — 결정사항 줄 단위 → 태스크 일괄 생성
- [x] 회의 이벤트 → `activity_logs` 자동 기록 ✅ (MEETING_CREATE/UPDATE/DELETE/CHECKIN)
- **참조:** `backend/modules/meeting.md`

### ✅ Phase 3 Meeting API 검증 완료 (2026-03-30)
> 전체 13개 테스트 통과 (`meeting-test.mjs`):
> - `POST /meetings` ✅ 201 (회의 생성 + 체크인코드 자동 생성)
> - `GET /meetings` ✅ 200 (목록, meetingDate 내림차순)
> - `GET /meetings/:id` ✅ 200 (상세)
> - `PATCH /meetings/:id` ✅ 200 (수정 — notes, decisions 저장)
> - `POST /meetings/:id/checkin` ✅ 200 (user1 체크인)
> - `POST /meetings/:id/checkin` ✅ 200 (user2 체크인)
> - `POST /meetings/:id/checkin` ✅ 409 (중복 체크인 거부)
> - `POST /meetings/:id/checkin` ✅ 400 (잘못된 코드 거부)
> - `GET /meetings/:id` ✅ 200 (체크인 후 참석자 2명 확인)
> - `DELETE /meetings/:id` ✅ 200 (삭제)
> - `GET /meetings` ✅ 200 (삭제 후 1개 확인)
> - `GET /meetings` ✅ 403 (비멤버 접근 거부)

### 🟡 P1 — 프론트엔드: 칸반 보드 ✅ (2026-03-30)
- [x] 3단 칼럼 레이아웃 (To Do / In Progress / Done) ✅
- [x] 태스크 카드 컴포넌트 ✅
- [x] 드래그 앤 드롭 (@dnd-kit) ✅
- [x] 태스크 생성/편집 모달 ✅
- [x] 필터 (담당자, 태그, 우선순위) ✅ (2026-04-06)
- **참조:** `frontend/pages/board.md`

### 🟡 P1 — 프론트엔드: 회의록 ✅ (2026-03-30)
- [x] 회의 목록 페이지 ✅
- [x] 회의 상세 (참석자, 내용, 결정사항) ✅
- [x] 체크인 UI (코드 입력 모달) ✅
- [x] 액션 아이템 → 태스크 생성 버튼 ✅ (2026-04-06)
- **참조:** `frontend/pages/meeting.md`

---

## Phase 4: Hash Vault & Score Engine (5~7주, Phase 3과 병행) — ✅ 완료 (2026-03-30)

### 🔴 P0 — 백엔드: Hash Vault (로컬 파일 저장) ✅ (2026-03-30)
- [x] FileStorageService (로컬 디스크 저장: `/data/uploads/{projectId}/`) ✅
- [x] SHA-256 해시 생성 (HashService) ✅
- [x] 파일 업로드 API (`POST /projects/:id/files` — multipart) ✅
- [x] `file_vault` INSERT + 버전 관리 ✅
- [x] 재업로드 시 해시 비교 로직 (중복=기존 반환, 변경=새 버전+변조 로그) ✅
- [x] 파일 다운로드 API (`GET /files/:id/download` — 스트리밍) ✅
- [x] 변조 감지 시 `tamper_detection_log` 기록 ✅
- [x] 파일 이력 조회 API ✅
- **참조:** `backend/modules/vault.md`

### ✅ Phase 4 Hash Vault API 검증 완료 (2026-03-30)
> 전체 9개 테스트 통과 (`vault-test.mjs`):
> - 파일 업로드 201 (SHA-256 + 버전1) ✅
> - 중복 업로드 → 기존 반환 (동일 ID) ✅
> - 동일 파일명 + 다른 내용 → 버전2 + 변조 감지 ✅
> - 파일 목록 200 ✅
> - 파일 상세 200 ✅
> - 파일 이력 200 (2개 버전) ✅
> - 파일 다운로드 200 ✅
> - 다른 파일 업로드 201 ✅
> - 비멤버 접근 403 ✅

### 🔴 P0 — 백엔드: Score Engine (MVP 버전) ✅ (2026-03-30)
- [x] 플랫폼 활동 로그 기반 점수 산출 ✅
- [x] 팀 평균 기준 정규화 (상한 150 클리핑) ✅
- [x] 항목별 점수 계산 (태스크/회의/파일/Git) ✅
- [x] 종합 점수 = Σ(항목 × 가중치) ✅
- [x] 점수 재계산 API (수동 트리거) ✅
- [x] 기본 가중치 설정 (w1=0.30, w2=0.25, w3=0.20, w4=0.25) ✅
- [x] 가중치 변경 API (교수/TA 전용) + 변경 로그 기록 ✅
- **참조:** `backend/modules/score.md`

### ✅ Phase 4 Score Engine API 검증 완료 (2026-03-30)
> 전체 16개 테스트 통과 (`score-test.mjs`):
> - 전체 점수 조회 200 (자동 계산) ✅
> - 활발한 학생 점수 > 0 ✅
> - 비활동 학생 점수 = 0 ✅
> - 내 점수 조회 200 ✅
> - 가중치 조회 200 (기본값) ✅
> - 가중치 변경 200 (교수) ✅
> - 학생 가중치 변경 거부 403 ✅
> - 잘못된 가중치 합 거부 400 ✅
> - 재계산 200 (변경된 가중치 반영) ✅
> - 비멤버 접근 403 ✅

### 🔴 P0 — 백엔드: 경보 시스템 (규칙 기반) ✅ (2026-03-30)
- [x] 불균형 감지 (FREE_RIDE: 팀 평균의 40% 이하) ✅
- [x] 이탈 감지 (DROPOUT: 2주 연속 활동 없음) ✅
- [x] 과부하 감지 (OVERLOAD: 1인이 60% 이상) ✅
- [x] 점수 조작 의심 감지 (GAMING_SUSPECT: 30분 내 15건+) ✅
- [x] 경보 생성 → `alerts` 테이블 ✅
- [x] 경보 조회/읽음처리/전체읽음 API ✅
- **참조:** `backend/modules/alert.md`

### ✅ Phase 4 Alert System API 검증 완료 (2026-03-30)
> 전체 15개 테스트 통과 (`alert-test.mjs`):
> - 경보 감지 실행 200 (FREE_RIDE + OVERLOAD 2건) ✅
> - 전체 경보 조회 200 ✅
> - 읽지 않은 경보 조회 200 ✅
> - 카운트 조회 200 ✅
> - 읽음 처리 200 + 카운트 감소 ✅
> - 전체 읽음 200 + 카운트 = 0 ✅
> - 비멤버 접근 403 ✅

### 🟡 P1 — 프론트엔드: 파일 & 점수 ✅ (2026-03-30)
- [x] 파일 업로드 UI (드래그앤드롭, 다중파일) ✅
- [x] 파일 이력 뷰 (버전별 해시/크기/업로더 표시) ✅
- [x] 파일 다운로드 (blob 스트리밍) ✅
- [x] 내 기여도 요약 카드 (점수 페이지 + 레이더 차트) ✅
- **참조:** `frontend/pages/vault.md`, `frontend/pages/analytics.md`

---

## Phase 5: 교수 대시보드 & MVP 마무리 (7~8주) — 프론트엔드 ✅ 완료 (2026-03-30)

### 🔴 P0 — 프론트엔드: 교수 대시보드 ✅ (2026-03-30)
- [x] 팀 목록 오버뷰 (카드 뷰, 건강도 기준 정렬) ✅
- [x] 팀 상세: 기여도 바 차트 (Recharts BarChart + RadarChart) ✅
- [x] 프로젝트 진행률 표시 (ProgressBar done/total) ✅
- [x] 경보 뱃지 & 목록 (unread count, 타입별 아이콘/색상) ✅
- [x] 건강도 지표 (🟢양호 🟡주의 🔴위험) ✅
- [x] 백엔드: Dashboard Overview API (`GET /api/dashboard/overview`) ✅
- **참조:** `frontend/pages/professor.md`

### 🔴 P0 — Docker 배포 안정화
- [ ] Docker Compose production 프로필 구성
- [ ] Nginx SSL 설정 (self-signed for demo)
- [ ] `docker compose -f docker-compose.prod.yml up -d` 검증
- [ ] 데모 환경에서 전체 플로우 동작 확인

### 🔴 P0 — 통합 & 중간발표 준비
- [ ] 프론트-백 통합 테스트
- [ ] 데모 시나리오 리허설
- [ ] 버그 수정 & 안정화
- [ ] **8주차 중간발표 데모:**
  1. `docker compose up` 으로 전체 스택 기동 시연
  2. 프로젝트 생성 → 팀원 초대
  3. 칸반 태스크 생성/완료
  4. 회의록 작성 + 체크인
  5. 파일 업로드 → 해시 고정 시연
  6. 교수 대시보드에서 기여도 확인
  7. (보너스) 해시 변조 감지 데모

---

## Phase 6: 확장 1 — 외부 연동 (9~12주)

### 🟡 P1 — GitHub App 연동
- [ ] GitHub App 등록 & 설정
- [ ] Installation webhook 수신 엔드포인트
- [ ] Push/PR/Issue webhook 파싱
- [ ] 커밋 데이터 → `activity_logs` (source: GITHUB)
- [ ] 폴백 폴링 (30분 보정)
- **참조:** `backend/modules/collector.md`

### 🟡 P1 — Google Drive 연동
- [ ] Google OAuth 2.0 연동
- [ ] Drive Push Notification (Changes: watch)
- [ ] Revision history 수집
- [ ] 댓글 수집
- [ ] 데이터 → `activity_logs` (source: GOOGLE_DRIVE)
- **참조:** `backend/modules/collector.md`

### 🟡 P1 — Score Engine 확장
- [ ] 외부 데이터 통합 점수 산출
- [ ] Git 기여 점수 세부 수식
- [ ] 문서 기여 점수 세부 수식
- [ ] 신뢰도 가중치 적용 (자동 1.0, 수동 0.7)
- **참조:** `backend/modules/score.md`

### 🟡 P1 — 프론트: 확장 UI
- [ ] 외부 연동 설정 페이지
- [ ] 활동 타임라인 (소스별 색상 구분)
- [ ] 교수 상세 대시보드 확장

---

## Phase 7: 확장 2 — AI & 고도화 (13~15주, 시간 허용 시)

### 🟢 P2 — AI Analyzer
- [ ] Claude API 연동
- [ ] 커밋 품질 분석 (배치 10건 단위)
- [ ] quality_score 산출 & `activity_logs` 업데이트
- [ ] Anti-Gaming 로직
- [ ] AI 분석 동의 플로우 (Step 4)
- **참조:** `backend/modules/analyzer.md`

### 🟢 P2 — 피어리뷰 & 리포트
- [ ] 피어리뷰 제출/결과 API
- [ ] 시스템 점수 vs 피어리뷰 크로스체크
- [ ] PDF 리포트 자동 생성
- [ ] 교수 가중치 조정 UI (슬라이더 + 프리셋)
- [ ] 가중치 변경 이력

---

## 우선순위 범례

| 레벨 | 의미 | 시점 |
|------|------|------|
| 🔴 P0 | MVP 필수 — 이것 없으면 중간발표 불가 | 1~8주 |
| 🟡 P1 | 핵심 차별화 — 외부 연동으로 제품 완성도 확보 | 9~12주 |
| 🟢 P2 | 고도화 — 시간 허용 시 추가, 없어도 시스템 동작 | 13~15주 |

---

## 의존성 그래프

```
Docker Compose ──→ DB 스키마 (Flyway) ──→ 백엔드 인증 ──→ 프로젝트 API
     │                                          │              │
     ↓                                          ↓              ↓
  Nginx 설정                              프론트 인증     태스크 API
                                                │              │
                                                ↓              ↓
                                          프로젝트 UI    칸반 보드 UI
                                                               │
                                                          회의록 API/UI
                                                               │
                                                          Hash Vault
                                                         (로컬 파일 저장)
                                                               │
                                                          Score Engine
                                                               │
                                                          교수 대시보드
                                                               │
                                                         [MVP 완성] ←─ Docker 배포 안정화
                                                               │
                                                    [확장 1] GitHub/Drive 연동
                                                               │
                                                    [확장 2] AI Analyzer, 피어리뷰
```
