# Team Blackbox — 기술 선택 기록 (Decision Log)

## 이 파일의 목적
프로젝트 구축 과정에서 여러 선택지가 있었던 항목들을 기록한다.
각 선택의 배경, 대안, 최종 결정 사유를 남겨 향후 "왜 이렇게 했지?"를 방지한다.

---

## Phase 1: 인프라 & 기반 구축

### SEL-01: 백엔드 빌드 도구
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ Gradle (Groovy DSL)** | docker.md 기존 설계 일치, 빌드 빠름, 스크립트 간결 | Groovy 문법 학습 필요 |
| Gradle (Kotlin DSL) | 타입 안전, IDE 자동완성 | 빌드 느림, 학습 곡선 |
| Maven | 전통적, XML 기반, 레퍼런스 풍부 | 빌드 느림, XML 장황 |

**결정:** Gradle (Groovy DSL) — docker.md에 정의된 Dockerfile이 `gradlew` 기반이며 팀 학습 비용 최소화

---

### SEL-02: Frontend 패키지 매니저
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ npm** | Node.js 기본 포함, 별도 설치 불필요, Dockerfile 호환 | pnpm보다 느림, 디스크 덜 효율적 |
| pnpm | 빠르고 디스크 효율적, strict 의존성 | 별도 설치 필요, Dockerfile 수정 필요 |
| yarn | 병렬 설치, lock 파일 안정적 | npm과 기능 차이 줄어듦, 버전 혼란 (classic vs berry) |

**결정:** npm — docker.md의 Dockerfile이 `npm ci` / `npm run build` 기준, 추가 설치 불필요

---

### SEL-03: Java 배포판
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ Eclipse Temurin (Adoptium)** | Docker 이미지 공식 제공, 커뮤니티 표준 | 없음 |
| Amazon Corretto | AWS 최적화, LTS 지원 | AWS 생태계 종속 느낌 |
| Oracle JDK | 공식 JDK | 라이선스 제약 |

**결정:** Eclipse Temurin — Dockerfile이 `eclipse-temurin:25-jdk-alpine` 기반, 업계 표준

---

### SEL-04: Git 브랜치 전략
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ GitHub Flow** | 단순 (main + feature branches), 소규모 팀 적합 | release 브랜치 없음 |
| Git Flow | 체계적 (develop/release/hotfix), 대규모 적합 | 4인 팀에 과도한 복잡도 |
| Trunk-Based | 매우 단순, CI/CD와 잘 맞음 | 기능 격리 어려움 |

**결정:** GitHub Flow — 4인 팀 규모에 적합, main + feature branch로 충분, PR 기반 코드리뷰

---

### SEL-05: Spring Boot 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ Spring Boot 3.4.x** | 최신 안정 버전, Java 25 지원, 활발한 지원 | 없음 |
| Spring Boot 3.2.x | 검증된 버전 | 일부 최신 기능 미포함 |

**결정:** Spring Boot 3.4.4 — 2026년 3월 기준 최신 안정 릴리스, Java 25 호환

---

### SEL-06: Next.js 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ Next.js 14** | 안정적 App Router, standalone 빌드 지원, 레퍼런스 많음 | 없음 |
| Next.js 15 | 최신, React 19 | 일부 라이브러리 호환성 이슈 가능 |

**결정:** Next.js 14 — claude.md 명세 (Next.js 14+), App Router 안정성 확보

---

### SEL-07: 개발 시 실행 방식
| 선택지 | 장점 | 단점 |
|--------|------|------|
| **✅ 하이브리드 (DB=Docker, App=호스트)** | Hot reload 지원, 개발 속도 빠름 | docker-compose.yml과 별도 명령 필요 |
| 전체 Docker 내부 | 환경 일관성 100% | 코드 변경 시 재빌드 필요, 느림 |
| 전부 호스트 | 빠른 개발 | PostgreSQL 별도 설치 필요, 환경 차이 |

**결정:** 하이브리드 — docker.md에 이미 기술된 방식. 개발 시 `docker compose up -d db`로 DB만 띄우고, backend/frontend는 호스트에서 직접 실행. 배포/통합 테스트 시 전체 Docker.

---

### SEL-08: Java 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| Java 17 (LTS) | 장기 지원, 안정적, 레퍼런스 풍부 | 최신 언어 기능 미포함 |
| Java 21 (LTS) | LTS, 가상 스레드, 패턴 매칭 | 없음 |
| **✅ Java 25** | 최신 기능 (값 클래스, 구조적 동시성 등), Temurin 지원 | non-LTS, 6개월 지원 |

**결정:** Java 25 (Eclipse Temurin 25.0.2) — 사용자 요청에 따라 최신 버전 채택. non-LTS이나 캡스톤 프로젝트 기간(~15주)에는 충분.

---

### SEL-09: Node.js 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| Node.js 20 (LTS) | 장기 지원, 검증, docker.md 원안 | 일부 최신 API 미포함 |
| Node.js 24 (LTS) | 최신 LTS | 없음 |
| **✅ Node.js 25** | 최신 런타임, 성능 최적화 | non-LTS, Current 릴리스 |

**결정:** Node.js 25.8.1 — 사용자 요청에 따라 최신 버전 채택. Docker 이미지 `node:25-alpine` 사용.

---

### SEL-10: Gradle 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| Gradle 8.13 | docker.md 원안 | Java 25 미지원 (class file version 69 오류) |
| **✅ Gradle 9.4.1** | Java 25/26 공식 지원, 2026-03 최신 | Gradle 9.x 마이그레이션 필요 |

**결정:** Gradle 9.4.1 — Java 25와의 호환성 필수. Gradle 8.13은 `Unsupported class file major version 69` 오류 발생.

---

### SEL-11: Lombok 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| Spring Boot 관리 버전 (1.18.34) | 별도 설정 불필요 | Java 25 비호환 (`NoSuchFieldException: TypeTag::UNKNOWN`) |
| **✅ Lombok 1.18.44** | Java 25 호환, 최신 릴리스 | 버전 수동 명시 필요 |

**결정:** Lombok 1.18.44 — Java 25 내부 API 변경으로 인해 기본 버전 비호환. 수동으로 최신 버전 명시.

---

### SEL-12: Next.js 패치 버전
| 선택지 | 장점 | 단점 |
|--------|------|------|
| Next.js 14.2.21 | 원래 설정 | 보안 취약점 (CVE, npm audit critical) |
| **✅ Next.js 14.2.35** | 보안 패치 적용, 최신 14.2.x | 없음 |

**결정:** Next.js 14.2.35 — npm audit에서 보안 취약점 감지. 최신 패치 버전으로 업그레이드.

---

### SEL-13: WSL2 활성화 방법
| 선택지 | 장점 | 단점 |
|--------|------|------|
| `wsl --install` (대화형) | 간단 | 비관리자 터미널에서 UAC 처리 불가 |
| `winget install Microsoft.WSL` | 패키지 매니저 | WSL 바이너리만 설치, 커널 기능 미활성화 |
| **✅ DISM 관리자 실행** | 커널 기능 직접 활성화 | 재부팅 필요 |

**결정:** `Start-Process -Verb RunAs`로 관리자 PowerShell에서 DISM 실행 → `Microsoft-Windows-Subsystem-Linux` + `VirtualMachinePlatform` 기능 활성화. 재부팅 후 Docker Desktop WSL2 백엔드 정상 작동.

### SEL-14: gradlew CRLF 문제 해결
| 선택지 | 장점 | 단점 |
|--------|------|------|
| `.gitattributes`에 LF 강제 | 근본적 해결 | Git 클론 후에야 적용 |
| **✅ Dockerfile 내 `sed -i 's/\r$//'`** | Docker 빌드 시점에 확실히 해결 | 매 빌드마다 실행 (무시할 수준) |
| `dos2unix` 수동 변환 | 간단 | Windows 개발자마다 반복 필요 |

**결정:** Alpine 기반 Docker 이미지에서 Windows CRLF 줄바꿈(`\r`) 때문에 `./gradlew: not found` 에러. Dockerfile의 `RUN` 단계에서 `sed`로 CR 제거 후 실행하도록 수정. 모든 OS에서 동일하게 동작.
