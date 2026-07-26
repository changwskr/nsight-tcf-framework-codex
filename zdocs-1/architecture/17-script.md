# 17. 스크립트 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 17 |
| 제목 | Script Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [16-deploy.md](16-deploy.md), [13-batch.md](13-batch.md), [ztomcat/README.md](../../ztomcat/README.md) |
| 대상 | 개발/배포/운영 담당자 |

---

## 1. 개요

NSIGHT TCF 스크립트는 목적에 따라 3계층으로 구성된다.

| 계층 | 위치 | 역할 |
|------|------|------|
| **공통 개발 스크립트** | `tcf-scripts/` | 로컬 기동·빌드·샘플 호출·Tomcat 배포 래퍼 |
| **통합 배포 스크립트** | `ztomcat/` | Tomcat 설치/기동/중지/WAR 배포/검증/재배포 |
| **모듈별 스크립트** | `*/scripts/` | 단일 모듈 build/run/deploy 편의 |

핵심 목표:

1. 명령 표준화(개인별 수동 절차 차이 제거)
2. OS 이식성(Windows + Linux/macOS)
3. 배포 안정성(검증·재시도·원클릭 재배포)

---

## 2. 전체 실행 흐름

```text
개발 단계
  tcf-scripts/run-local
  tcf-scripts/build
  tcf-scripts/curl-sample

통합 배포 단계
  ztomcat/install-tomcat
  ztomcat/deploy-wars
  ztomcat/start
  ztomcat/verify-deploy
  (필요 시) ztomcat/deploy-restart

모듈 단독 단계
  {module}/scripts/build|run-local|deploy
```

---

## 3. `tcf-scripts` 아키텍처 (공통 오케스트레이션)

`tcf-scripts`는 루트 실행 기준의 **상위 래퍼 계층**이다.

## 3.1 주요 스크립트

| 스크립트 | 역할 |
|----------|------|
| `run-local.bat/.sh` | bootRun 실행(단일/복수/all) |
| `build.bat/.sh` | 모듈/전체 빌드, WAR 집합 빌드 |
| `curl-sample.bat/.sh` | 샘플 전문 호출 |
| `deploy.bat/.sh` | WAR 빌드 후 ztomcat webapps 복사 |

## 3.2 `run-local` 설계 포인트

- 단일 인자: 포그라운드 실행
- 복수 인자: 새 창(백그라운드) 분산 실행
- `all`: 업무 9개 + `tcf-om` 일괄 기동

지원 별칭:

- `sv`, `sv-service`
- `om`, `tcf-om`, `ud`
- `batch`, `tcf-batch`
- `ui`, `tcf-ui`

## 3.3 `build` 설계 포인트

주요 타겟:

| 타겟 | 의미 |
|------|------|
| `all` | clean + `buildBusinessWars` |
| `wars` | `buildBusinessWars` |
| `ztomcat` | `buildZtomcatWars` |
| `tcf` | core 계열 빌드 |
| `ui`, `batch` | 플랫폼 모듈 빌드 |

즉, Gradle task를 도메인 단위 명령으로 추상화한다.

---

## 4. `ztomcat` 아키텍처 (통합 배포 런타임)

`ztomcat`은 운영 유사 통합 테스트를 위한 **배포 실행기**다.

## 4.1 기능군

| 스크립트군 | 파일 | 역할 |
|-----------|------|------|
| 설치 | `install-tomcat.*` | Tomcat 10.1.34 설치 |
| 배포 | `deploy-wars.*` | WAR 빌드/복사(19 context) |
| 기동/중지 | `start.*`, `stop.*` | Tomcat lifecycle |
| 설정 적용 | `apply-config.*` | setenv 복사, UTF-8 connector 적용 |
| 검증 | `verify-deploy.*` | 19 context health 확인 |
| 원클릭 | `deploy-restart.*` | stop→deploy→start→verify |

## 4.2 OS 이슈 대응

- 경로에 괄호가 있는 Windows 환경 대응을 위해 `start.bat`/`stop.bat`가 PowerShell 래퍼(`start.ps1`/`stop.ps1`)를 사용
- Linux/macOS는 `*.sh`로 동일 시맨틱 제공

## 4.3 `deploy-wars` 설계

지원 코드:

```text
cc ic pc bc ms sv pd cm eb ep bp bd ss cs ct mg om batch ui
```

모드:

- 인자 없음/`all`: 19개 전체 배포
- 단건/복수: 선택 배포

단건 배포 시 exploded 디렉터리 정리 후 WAR 교체로 구버전 잔존을 방지한다.

---

## 5. 모듈별 `scripts/` 아키텍처

각 모듈은 자체 `scripts`를 가진다.

| 유형 | 대표 파일 | 역할 |
|------|-----------|------|
| 업무 서비스 | `{code}-service/scripts/build.*` | core 의존 + 해당 WAR 빌드 |
| 업무 서비스 | `{code}-service/scripts/run-local.*` | 단일 모듈 bootRun |
| 업무 서비스 | `{code}-service/scripts/deploy.*` | Tomcat webapps 단건 배포 |
| 플랫폼 | `tcf-om/scripts/*`, `tcf-ui/scripts/*` | 모듈별 build/run/deploy |
| 플랫폼 | `tcf-batch/scripts/run-local.bat` | batch bootRun |

의미:

- `tcf-scripts`가 상위 표준 인터페이스라면,
- 모듈 스크립트는 도메인 전용 빠른 경로(직접 작업)다.

---

## 6. Gradle Task 매핑 아키텍처

스크립트는 결국 Gradle 태스크를 호출한다.

| Gradle Task | 사용 스크립트 | 결과 |
|-------------|--------------|------|
| `:module:bootRun` | `run-local`, 모듈 `run-local` | 개발 실행 |
| `:module:bootWar` | 모듈 `build`/`deploy` | WAR 산출 |
| `buildBusinessWars` | `tcf-scripts/build wars`, `deploy` | 17 WAR |
| `buildZtomcatWars` | `ztomcat/deploy-wars all`, `tcf-scripts/build ztomcat` | 15 WAR 빌드 / 13 배포 |

즉, 스크립트는 “사람 친화 명령”, Gradle은 “실행 엔진” 역할을 담당한다.

---

## 7. 스크립트 간 역할 분리 원칙

1. **개발 기동**: `tcf-scripts/run-local`
2. **통합 배포**: `ztomcat/deploy-wars` + `verify-deploy`
3. **단일 모듈 반복 작업**: 해당 모듈 `scripts/`
4. **원클릭 통합 재배포**: `ztomcat/deploy-restart`

권장: 전체 통합 검증은 `ztomcat` 경로를 우선 사용한다.

---

## 8. 환경 변수/설정 주입

## 8.1 공통 포인트

| 항목 | 용도 |
|------|------|
| `GRADLE_HOME` / override | Gradle 실행 경로 제어 |
| `TOMCAT_WEBAPPS` | 배포 대상 webapps 경로 |
| `NSIGHT_TXLOG_PATH` | 공유 H2 경로 통일 |

## 8.2 ztomcat setenv 계층

| 파일 | 역할 |
|------|------|
| `conf/setenv.bat/.sh` | 기본 JVM/JDK/시스템 프로퍼티 템플릿 |
| `setenv.local.bat/.sh` | 로컬 사용자 오버라이드 |
| `apply-config.*` | 템플릿을 실제 Tomcat bin으로 반영 |

---

## 9. 운영 안정성 장치

| 장치 | 구현 |
|------|------|
| 배포 후 검증 | `verify-deploy`(health 19개) |
| 전체 재배포 표준 시퀀스 | `deploy-restart` |
| 인코딩 안전성 | `apply-config`에서 UTF-8 connector 강제 |
| JDK 버전 일치 | start 스크립트에서 JDK 21 경로 고정/검증 |

---

## 10. 대표 시나리오

## 10.1 개발자 일상

```text
run-local sv
curl-sample sv
build sv
```

## 10.2 통합 점검

```text
ztomcat/deploy-restart
ztomcat/verify-deploy
```

## 10.3 특정 모듈 긴급 패치

```text
{module}/scripts/build
{module}/scripts/deploy
ztomcat/verify-deploy
```

---

## 11. 주의사항

1. `om-service`는 레거시로, 표준 배포 파이프라인은 `tcf-om` 기준
2. 배포 모드에 따라 URL/포트/context가 달라짐(bootRun vs 8080 gateway)
3. `nsight.txlog.path` 불일치 시 OM/Batch/거래로그 데이터가 분리될 수 있음
4. 단건 배포 직후 autoDeploy 완료 대기 없이 검증하면 오탐 가능

---

## 12. 체크리스트

- [ ] 목적(개발/통합/긴급패치)에 맞는 스크립트 계층을 선택했는가
- [ ] build 결과 WAR 파일명이 context 규칙과 일치하는가
- [ ] 배포 후 `verify-deploy`를 수행했는가
- [ ] 공유 DB 경로(`NSIGHT_TXLOG_PATH`)가 일치하는가
- [ ] JDK 21 및 UTF-8 설정이 반영됐는가

---

## 13. 참고 소스

| 경로 | 설명 |
|------|------|
| `tcf-scripts/README.md` | 공통 스크립트 가이드 |
| `tcf-scripts/run-local.*` | 개발 기동 표준 |
| `tcf-scripts/build.*` | 빌드 표준 |
| `tcf-scripts/deploy.*` | Tomcat 배포 래퍼 |
| `ztomcat/README.md` | 통합 배포 절차 |
| `ztomcat/deploy-wars.*` | **13 WAR** 배포 |
| `ztomcat/deploy-restart.*` | 원클릭 재배포 |
| `*/scripts/` | 모듈별 스크립트 |

---

## 14. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 공통/통합/모듈 스크립트 아키텍처 정리 |
