# 참조 문서 및 코드 지도

## 첫 진입점

| 목적 | 경로 |
|---|---|
| 프로젝트 전체 개요 | `/README.md` |
| Gradle 공통 설정 | `/build.gradle`, `/settings.gradle` |
| TCF Core 안내 | `/tcf-core/README.md` |
| Web 통합 안내 | `/tcf-web/README.md` |
| 대표 업무 구현 | `/sv-service/` |
| 통합 Tomcat | `/ztomcat/README.md` |
| 빌드 및 배포 Script | `/tcf-scripts/README.md` |

## 핵심 코드 진입점

| 관심사 | 코드 |
|---|---|
| HTTP 거래 진입 | `tcf-web/.../OnlineTransactionController.java` |
| 내부 거래 진입 | `tcf-web/.../TcfGateway.java` |
| 거래 오케스트레이션 | `tcf-core/.../processor/TCF.java` |
| 전처리 | `tcf-core/.../processor/STF.java` |
| 후처리 | `tcf-core/.../processor/ETF.java` |
| Handler 등록 및 실행 | `tcf-core/.../dispatch/TransactionDispatcher.java` |
| Handler 계약 | `tcf-core/.../transaction/TransactionHandler.java` |
| 온라인 타임아웃 | `tcf-core/.../timeout/OnlineTransactionTimeoutExecutor.java` |
| Web 자동 설정 | `tcf-web/.../config/TcfAutoConfiguration.java` |
| WAR Bootstrap | `tcf-web/.../support/NsightWarBootstrap.java` |

`...`는 `src/main/java/com/nh/nsight` 이하 패키지 경로를 생략한 표기다.

## 상세 문서 체계

| 디렉터리 | 역할 |
|---|---|
| `zarchitecture` | 영역별 아키텍처 요약과 코드 기준 설계 |
| `zdocs-1/architecture` | 세부 기술 아키텍처 문서 |
| `zdocs-1/manual` | Gradle, 환경변수와 산출물 매뉴얼 |
| `zguide` | 모듈별 개발 가이드 |
| `zman` | 공식 설계서 요약 및 설계-코드 비교 |
| `znsight-man` | Markdown 개발 매뉴얼 |
| `ztcfbook*` | 교육 및 개발북 자료 |
| `tcf-help` | UI에서 사용하는 도움말 카탈로그와 색인 |

## AI 개발 도구

| 도구 | 역할 | 기본 포트 |
|---|---|---:|
| `tcf-ai-methodology` | 업무 모델 정의, 검증 및 코드 초안 생성 | 8787 |
| `tcf-ai-crud-meoy` | C-MASTER/C00–C18 CRUD 개발 절차 관리 | 8788 |

생성 결과는 반드시 Diff, Compile, Test와 코드 리뷰를 거쳐야 한다.

## 대표 실행 명령

```powershell
# 전체 빌드
.\gradlew.bat build

# Core 테스트
.\gradlew.bat :tcf-core:test

# SV 업무 실행
.\gradlew.bat :sv-service:bootRun

# 업무 WAR 생성
.\gradlew.bat buildBusinessWars

# 통합 Tomcat 대상 WAR 생성
.\gradlew.bat buildZtomcatWars
```

## 문서 확인 우선순위

1. 실제 코드와 Gradle 설정
2. 자동화 테스트
3. Profile 및 배포 설정
4. 모듈 README와 `xdoc`
5. 세부 설계서와 과거 개발 자료

내용이 충돌하면 최신 코드 동작을 검증한 뒤 관련 문서를 함께 갱신한다.

