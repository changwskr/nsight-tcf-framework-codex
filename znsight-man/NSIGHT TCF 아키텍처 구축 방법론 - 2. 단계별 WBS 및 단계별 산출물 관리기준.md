NSIGHT TCF 개발방법론

단계별 WBS 및 단계별 산출물 관리기준

1. 도입 전 안내말

NSIGHT 프로젝트의 WBS는 단순히 작업명과 일정을 나열하는 일정표가 아니다.

각 작업은 다음 요소가 연결된 아키텍처 실행계획이어야 한다.

```
WBS 작업
→ 책임자
→ 입력자료
→ 수행 활동
→ 의사결정
→ 산출물
→ 검증 증적
→ Architecture Gate
```

NSIGHT 아키텍처 구축 방법론은 G0 거버넌스부터 G14 안정화·지속 개선까지 15단계로 구성되며, 각 단계는 입력자료, 분석·설계 활동, 의사결정, 산출물, 검증 증적, Gate 통과 순서로 관리하도록 정의되어 있다.

따라서 다음 상태만으로는 WBS 작업이 완료된 것으로 보지 않는다.

```
문서 초안 작성 완료
코드 작성 완료
서버 설치 완료
테스트 실행 완료
```

다음 조건까지 만족해야 작업이 완료된다.

```
산출물 작성
+ 내부검토
+ 관련 영역 정합성 확인
+ 결함 조치
+ 승인 또는 Baseline
+ 추적정보 등록
```

NSIGHT에서는 특히 다음 연결관계를 기준으로 산출물 완전성을 판단한다.

```
요구사항
↔ 업무기능·도메인
↔ 화면·이벤트
↔ ServiceId·거래코드
↔ Handler·Facade·Service·Rule·DAO·Mapper
↔ SQL·DB 객체
↔ OM 기준정보
↔ 테스트케이스
↔ 거래로그·감사로그
↔ 운영 Runbook
```

2. 문서 개요

2.1 목적

본 문서의 목적은 NSIGHT 프로젝트의 아키텍처 구축 방법론을 실행 가능한 WBS로 분해하고, 각 단계에서 작성·검토·승인해야 하는 산출물을 정의하는 것이다.

| 목적 | 설명 |
| --- | --- |
| 실행계획 구체화 | G0~G14 단계를 실제 수행 작업으로 분해 |
| 책임 명확화 | 작업별 주관·협의·승인 책임 정의 |
| 선후관계 관리 | 개발 이전에 확정되어야 할 설계항목 식별 |
| 산출물 누락 방지 | 단계별 필수 산출물과 검증 증적 정의 |
| Gate 운영 | 다음 단계 진입조건을 객관적으로 판단 |
| 추적성 확보 | 요구사항부터 운영 증적까지 연결 |
| 변경관리 | Baseline 이후 변경의 영향과 승인 통제 |
| PMO 관리 | 진척률과 완료 여부의 객관적 판단기준 제공 |

2.2 적용범위

- 프로젝트 관리 및 아키텍처 거버넌스
- 업무·애플리케이션·데이터·기술 아키텍처
- TCF 플랫폼 및 업무 WAR
- Gateway·JWT·OM·EAI·Batch·Cache
- UI·화면·거래·프로그램·SQL 설계
- 용량·성능·가용성·DR
- 개발환경·CI/CD·배포
- 통합·성능·보안·장애 시험
- 운영전환·초기 안정화
- 변경·호환성·폐기 관리
2.3 역할 약어

| 약어 | 역할 |
| --- | --- |
| PMO | 프로젝트 관리·품질·진척 통제 |
| SA | 시스템 아키텍트·통합 아키텍처 |
| AA | 애플리케이션 아키텍트 |
| DA | 데이터 아키텍트 |
| TA | 기술·인프라 아키텍트 |
| SEC | 보안 아키텍트·보안 담당 |
| FW | TCF 프레임워크 개발팀 |
| BA | 업무 분석·업무 설계팀 |
| DEV | 업무 애플리케이션 개발팀 |
| UI | UI·WEBTOPSUITE·React 개발팀 |
| DBA | 데이터베이스 운영·SQL 검토 |
| DVO | DevOps·CI/CD 담당 |
| QA | 테스트·품질 담당 |
| OPS | 운영·장애 대응 담당 |

3. WBS 운영 원칙

3.1 WBS 코드체계

```
G{단계번호}.{작업순번}
```

예:

```
G6.03  ServiceId·거래코드 표준 설계
G10.05 Timeout·오류·Rollback 선도검증
G13.07 Go/No-Go 심의
```

하위 작업이 필요한 경우 다음 형식을 사용한다.

```
G7.04.01 논리 데이터 모델 작성
G7.04.02 물리 데이터 모델 작성
G7.04.03 인덱스·파티션 설계
```

3.2 작업 완료율 기준

| 상태 | 완료율 | 판정기준 |
| --- | --- | --- |
| 미착수 | 0% | 작업 시작 전 |
| 착수 | 10% | 담당자·입력자료 확보 |
| 작성 중 | 30% | 초안 작성 또는 구현 진행 |
| 자체검토 | 50% | 수행 조직 내부검토 완료 |
| 통합검토 | 70% | 관련 아키텍처·업무·운영 검토 중 |
| 보완완료 | 90% | 지적사항 반영 및 증적 확보 |
| 승인완료 | 100% | 승인·Baseline·저장소 등록 완료 |

단순히 문서 파일이나 코드가 존재한다는 이유로 100% 완료 처리하지 않는다.

3.3 산출물 등급

| 등급 | 의미 | 관리방식 |
| --- | --- | --- |
| B Baseline | 프로젝트 공식 기준본 | 변경통제와 승인 필요 |
| D Design | 상세 설계 산출물 | 관련 아키텍트 검토 |
| R Register | 목록·대장·매트릭스 | 지속 갱신 |
| E Evidence | 시험·검증·승인 증적 | Gate 판단에 사용 |
| O Operation | 운영 절차·Runbook | 운영부서 인수 |
| T Template | 표준 템플릿 | 프로젝트 전체 공통 사용 |

3.4 작업 선후관계

```
G0 거버넌스
  ↓
G1 목표·범위
  ↓
G2 현행분석
  ↓
G3 업무·도메인
  ↓
G4 요구사항 Baseline
  ↓
G5 목표 아키텍처
  ├─→ G6 TCF·개발표준
  ├─→ G8 용량·물리구조
  └─→ G9 개발·배포 기반
           ↓
       G7 상세설계
           ↓
       G10 선도개발
           ↓
       G11 본 개발
           ↓
       G12 종합검증
           ↓
       G13 운영전환
           ↓
       G14 안정화
```

G6·G8·G9는 G5 목표 아키텍처 승인 이후 병행할 수 있다. G7 상세설계는 영역별로 순차 승인하여 개발과 부분 병행할 수 있지만, 미승인 설계를 전제로 한 개발은 원칙적으로 금지한다.

4. 단계별 Master WBS

| 단계 | 단계명 | 핵심 작업 | 주관 | 선행 단계 | Gate 결과 |
| --- | --- | --- | --- | --- | --- |
| G0 | 거버넌스 수립 | RACI, Gate, ADR, 변경·예외 체계 | SA·PMO | 없음 | 프로젝트 통제체계 승인 |
| G1 | 비전·목표·범위 | 목표, KPI, 범위, 로드맵 | SA·BA | G0 | 범위 Baseline |
| G2 | 현행·제약 분석 | AS-IS, 문제점, 기술부채 | SA 공동 | G1 | 현행·Gap 승인 |
| G3 | 업무·도메인 설계 | 기능, 도메인, 데이터 소유권 | AA·BA·DA | G2 | 업무경계 승인 |
| G4 | 요구사항 Baseline | 기능·비기능·품질속성 | BA·SA | G3 | 요구사항 승인 |
| G5 | 목표 아키텍처 | Big Picture, View, ADR | SA 공동 | G4 | 목표 구조 승인 |
| G6 | TCF·개발표준 | TCF, 전문, ServiceId, 계층, OM | AA·FW | G5 | 표준 Baseline |
| G7 | 상세설계 | 화면·거래·프로그램·DB·연계·보안 | AA·DA·SEC | G5·G6 | 상세설계 승인 |
| G8 | 용량·가용성·DR | TPS, VM, JVM, Pool, HA, DR | TA·AA·DA | G4·G5 | 물리·용량 승인 |
| G9 | 개발·배포 기반 | 저장소, 환경, CI/CD, 롤백 | DVO·TA·AA | G5·G6 | 개발기반 승인 |
| G10 | 선도개발 | 대표 거래와 전체 구조 검증 | AA·FW·DEV | G6~G9 | 실행 가능성 승인 |
| G11 | 본 개발 | 전체 구현, OM 등록, 품질 Gate | DEV·FW | G7·G10 | 개발완료 승인 |
| G12 | 종합검증 | 통합·성능·보안·장애·DR 시험 | QA·SA 공동 | G11 | 운영 후보 승인 |
| G13 | 운영전환 | 이행, 배포, Runbook, Go/No-Go | OPS·SA·PMO | G12 | Go-Live 승인 |
| G14 | 안정화·개선 | 집중관제, RCA, 기술부채, 폐기 | OPS·SA 공동 | G13 | 안정화 종료 |

5. G0 — 아키텍처 거버넌스 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G0.01 | 프로젝트 아키텍처 검토범위 정의 | SA | 채널·업무·데이터·플랫폼·인프라·운영 범위 확정 |
| G0.02 | 아키텍처 조직과 의사결정권자 지정 | PMO·SA | SA·AA·DA·TA·SEC 및 최종 승인자 지정 |
| G0.03 | 역할·책임 RACI 수립 | PMO·SA | 작성·검토·협의·승인 책임 확정 |
| G0.04 | Architecture Gate 체계 수립 | SA·QA | G0~G14 통과기준과 검토일정 승인 |
| G0.05 | 산출물 체계와 템플릿 정의 | SA·PMO | 산출물 ID, 템플릿, 저장위치, 버전 기준 확정 |
| G0.06 | ADR 관리체계 정의 | SA | 대안·판단근거·영향·폐기조건 템플릿 승인 |
| G0.07 | Risk·Gap·기술부채 관리체계 정의 | PMO·SA | 심각도, 담당자, 완료일, 종료기준 확정 |
| G0.08 | 아키텍처 예외 승인절차 정의 | SA·QA | 예외 사유, 승인자, 만료일, 개선계획 기준 확정 |
| G0.09 | 변경·Baseline 관리절차 정의 | PMO·SA | 변경요청, 영향분석, 승인, 배포 연결기준 확정 |
| G0.10 | G0 Gate 심의 | SA·PMO | 필수체계 100%, Blocker·Critical 0건 |

G0 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| GOV-01 | 아키텍처 관리계획서 | B | SA | 프로젝트 책임자 |
| GOV-02 | 프로젝트 아키텍처 RACI | B | PMO·SA | 프로젝트 책임자 |
| GOV-03 | Architecture Gate 정의서 | B | SA·QA | 아키텍처위원회 |
| GOV-04 | 산출물 목록 및 작성 템플릿 | T | SA·PMO | 품질책임자 |
| GOV-05 | ADR 템플릿 및 관리대장 | R | SA | 아키텍처위원회 |
| GOV-06 | Risk·Gap 관리대장 | R | PMO | SA |
| GOV-07 | 기술부채 관리대장 | R | AA·TA·DA | SA |
| GOV-08 | 아키텍처 예외 승인서 | T | QA·SA | 아키텍처위원회 |
| GOV-09 | 변경·Baseline 관리절차서 | B | PMO·SA | 프로젝트 책임자 |
| GOV-10 | G0 Gate 결과서 | E | QA | SA·PMO |

6. G1 — 비전·목표·범위 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G1.01 | 프로젝트 추진배경과 문제 정의 | BA·SA | 현행 문제와 구축 필요성 합의 |
| G1.02 | 프로젝트 비전 정의 | SA·BA | 목표 상태를 한 문장과 구조로 정의 |
| G1.03 | 정량적 성공 KPI 정의 | PMO·SA | 사용자·성능·가용성·운영 KPI 수치화 |
| G1.04 | 구축 대상 업무범위 정의 | BA | 포함·제외 업무와 단계별 적용범위 확정 |
| G1.05 | 채널·데이터·연계 범위 정의 | SA·DA | UI, DB, 외부시스템, 파일·배치 범위 확정 |
| G1.06 | 이해관계자와 사용자군 식별 | BA·PMO | 지점·본부·관리자·운영자 구분 |
| G1.07 | 단계별 구축 로드맵 수립 | PMO·SA | 1차·2차·후속 구축범위 구분 |
| G1.08 | 범위 제외 및 가정사항 등록 | PMO | 제외사유, 가정, 유효기간 기록 |
| G1.09 | 상위 요구사항 도출 | BA·SA | 업무·기술·운영 상위 요구사항 식별 |
| G1.10 | G1 Gate 심의 | SA·PMO | 목표·범위·KPI 주요 이해관계자 승인 |

G1 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| VIS-01 | 프로젝트 비전 및 목표서 | B | SA·BA | 프로젝트 책임자 |
| VIS-02 | 정량적 성공 KPI 정의서 | B | PMO·SA | 프로젝트 책임자 |
| VIS-03 | 시스템·업무 범위도 | B | SA | 아키텍처위원회 |
| VIS-04 | 대상·제외 업무 목록 | R | BA | 업무책임자 |
| VIS-05 | 이해관계자·사용자군 목록 | R | BA·PMO | PMO |
| VIS-06 | 채널·데이터·연계 범위표 | B | SA·DA | 아키텍처위원회 |
| VIS-07 | 단계별 구축 로드맵 | B | PMO·SA | 프로젝트 책임자 |
| VIS-08 | 가정·제약·제외사항 대장 | R | PMO | SA |
| VIS-09 | 상위 요구사항 목록 | R | BA | 업무책임자 |
| VIS-10 | G1 Gate 결과서 | E | QA | SA·PMO |

7. G2 — 현행·제약조건 분석 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G2.01 | 현행 채널·화면 구조 분석 | UI·AA | 사용자 진입과 화면 구성 현황 파악 |
| G2.02 | 현행 애플리케이션·모듈 분석 | AA | 시스템, WAR, Controller, 배포단위 목록화 |
| G2.03 | 현행 데이터·DB 구조 분석 | DA·DBA | RDW·ADW·업무 DB 역할과 데이터량 파악 |
| G2.04 | 현행 인터페이스 분석 | AA·DA | API·EAI·파일·DB 연계 목록 작성 |
| G2.05 | 현행 인증·세션·권한 분석 | SEC·AA | SSO, Session, JWT, 권한 책임 분석 |
| G2.06 | 현행 인프라·배포구조 분석 | TA | VM·JVM·Tomcat·WAR·Port 구조 작성 |
| G2.07 | 현행 운영·배포·모니터링 분석 | OPS·DVO | 로그·배포·백업·장애대응 현황 작성 |
| G2.08 | 장애·성능 이력 분석 | TA·AA·DA | Thread·GC·Pool·SQL·배포 장애 원인 분류 |
| G2.09 | 단일 장애점과 보안위험 식별 | TA·SEC | SPOF, 우회접근, 비밀정보 위험 등록 |
| G2.10 | 기술부채·유지·전환·폐기 분류 | SA 공동 | 목표구조 전환대상과 우선순위 도출 |
| G2.11 | AS-IS 종합구성도 작성 | SA | 채널부터 DB·운영까지 현행 구조 통합 |
| G2.12 | G2 Gate 심의 | SA | 현행 목록 완전성 및 핵심 Gap 승인 |

G2 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| ASI-01 | AS-IS 시스템 구성도 | B | SA | 아키텍처위원회 |
| ASI-02 | 현행 애플리케이션·WAR 목록 | R | AA | SA |
| ASI-03 | 현행 화면·채널 목록 | R | UI·BA | AA |
| ASI-04 | 현행 데이터·DB 현황서 | D | DA·DBA | DA |
| ASI-05 | 현행 인터페이스 목록 | R | AA·DA | SA |
| ASI-06 | 현행 인증·권한 흐름도 | D | SEC·AA | SEC |
| ASI-07 | 현행 Deployment 구성도 | D | TA | SA |
| ASI-08 | 장애·성능 이력 분석서 | E | TA·AA·DA | SA |
| ASI-09 | 단일 장애점·보안위험 목록 | R | TA·SEC | SA |
| ASI-10 | 기술부채 및 개선대상 대장 | R | SA 공동 | PMO·SA |
| ASI-11 | 유지·전환·폐기 분류표 | B | SA | 프로젝트 책임자 |
| ASI-12 | G2 Gate 결과서 | E | QA | SA |

8. G3 — 업무기능·도메인·데이터 소유권 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G3.01 | 업무기능 Level 1~5 분해 | BA | 업무영역부터 유스케이스까지 구조화 |
| G3.02 | 업무 용어사전 작성 | BA·DA | 화면·설계·코드·DB 공통 용어 확정 |
| G3.03 | 업무코드·업무세구분코드 후보 정의 | AA·BA | 코드별 책임과 적용범위 정의 |
| G3.04 | 업무 WAR 분리후보 도출 | AA·TA | 배포·장애·조직 경계를 고려한 후보 작성 |
| G3.05 | 업무 도메인·서브도메인 정의 | AA·BA | 도메인 목적, 규칙, 소유조직 정의 |
| G3.06 | 도메인 Context Map 작성 | AA | 제공·소비·공유·외부 관계 정의 |
| G3.07 | 데이터 소유권 정의 | DA·BA | 생성·변경·조회·폐기 책임 지정 |
| G3.08 | 화면·기능·이벤트 후보 도출 | BA·UI | 사용자 기능과 유스케이스 연결 |
| G3.09 | 거래·ServiceId 후보 도출 | AA·BA | 도메인별 거래 후보 식별 |
| G3.10 | 도메인 간 연동 후보 정의 | AA·DA | 동기·비동기·Read Model 후보 구분 |
| G3.11 | 도메인별 책임조직 RACI 작성 | PMO·AA | 설계·개발·데이터·운영 소유자 지정 |
| G3.12 | G3 Gate 심의 | SA·AA·DA | 업무·도메인·데이터 경계 승인 |

G3 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| DOM-01 | 업무기능 분해도 | B | BA | 업무책임자 |
| DOM-02 | 업무 용어사전 | B | BA·DA | 업무책임자·DA |
| DOM-03 | 업무코드·세구분코드 정의서 | B | AA·BA | SA |
| DOM-04 | 업무코드–WAR 매핑표 | B | AA·TA | SA |
| DOM-05 | 도메인·서브도메인 정의서 | B | AA·BA | SA·업무책임자 |
| DOM-06 | 도메인 Context Map | D | AA | SA |
| DOM-07 | 데이터 소유권 매트릭스 | B | DA·BA | DA·SA |
| DOM-08 | 화면·기능·이벤트 후보 목록 | R | BA·UI | AA |
| DOM-09 | 유스케이스·거래 후보 목록 | R | BA·AA | 업무책임자 |
| DOM-10 | 도메인 간 연동 후보 목록 | R | AA·DA | SA |
| DOM-11 | 도메인별 RACI | B | PMO·AA | SA |
| DOM-12 | G3 Gate 결과서 | E | QA | SA |

도메인은 단순 패키지명이 아니라 업무 용어·규칙·ServiceId·프로그램·데이터 소유권·권한·감사·책임조직을 함께 묶는 경계로 관리해야 한다.

9. G4 — 기능·비기능 요구사항 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G4.01 | 기능 요구사항 상세화 | BA | 요구사항 ID와 수용기준 정의 |
| G4.02 | 유스케이스·화면 이벤트 연결 | BA·UI | 요구사항과 사용자 이벤트 연결 |
| G4.03 | 사용자·동시성 요구 정의 | BA·TA | 총 사용자·동시사용·동시요청 기준 확정 |
| G4.04 | 성능·TPS·응답시간 요구 정의 | TA·AA | 평시·피크·스트레스 목표 수치화 |
| G4.05 | 가용성·RTO·RPO 요구 정의 | TA·DA | 업무·데이터별 복구기준 확정 |
| G4.06 | 데이터량·보관·파기 요구 정의 | DA·SEC | 초기량·증가율·보관기간 확정 |
| G4.07 | 배치·파일·대용량 요구 정의 | AA·TA·DA | 건수·크기·시간창·동시성 확정 |
| G4.08 | 보안·개인정보·감사 요구 정의 | SEC | 인증·권한·암호화·감사 기준 확정 |
| G4.09 | 운영·모니터링·배포 요구 정의 | OPS·DVO | 탐지·복구·배포·롤백 목표 확정 |
| G4.10 | 품질속성 시나리오 작성 | SA 공동 | 부하·장애·보안 상황별 기대결과 정의 |
| G4.11 | 검증방법·테스트 연결 | QA | 요구사항별 검증방식과 증적 정의 |
| G4.12 | RTM 및 요구사항 Baseline | BA·QA | 요구사항·설계·시험 연결 및 승인 |
| G4.13 | G4 Gate 심의 | SA·PMO | 미확정 핵심 NFR 0건 |

G4 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| REQ-01 | 기능 요구사항 명세서 | B | BA | 업무책임자 |
| REQ-02 | 비기능 요구사항 명세서 | B | SA 공동 | 프로젝트 책임자 |
| REQ-03 | 사용자·동시성 조건서 | B | BA·TA | SA |
| REQ-04 | 성능·TPS·응답시간 목표서 | B | TA·AA | SA |
| REQ-05 | 가용성·RTO·RPO 정의서 | B | TA·DA | SA |
| REQ-06 | 데이터량·보관·파기 요구서 | B | DA·SEC | SA |
| REQ-07 | 배치·파일·대용량 요구서 | D | AA·DA | SA |
| REQ-08 | 보안·개인정보·감사 요구서 | B | SEC | 보안책임자 |
| REQ-09 | 운영·모니터링·배포 요구서 | B | OPS·DVO | SA |
| REQ-10 | 품질속성 시나리오 | D | SA 공동 | SA |
| REQ-11 | 요구사항 추적 매트릭스 RTM | R | BA·QA | PMO |
| REQ-12 | 요구사항 Baseline 승인서 | E | PMO | 프로젝트 책임자 |
| REQ-13 | G4 Gate 결과서 | E | QA | SA |

10. G5 — 목표 아키텍처·대안 의사결정 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G5.01 | 시스템 Context View 설계 | SA | 사용자·채널·외부시스템·신뢰경계 정의 |
| G5.02 | 목표 Big Picture 설계 | SA | 채널부터 DB·운영까지 전체 구조 작성 |
| G5.03 | 논리 아키텍처 설계 | SA·AA | Gateway·TCF·WAR·OM·DB 역할 분리 |
| G5.04 | 애플리케이션 아키텍처 설계 | AA | 모듈·업무코드·도메인·계층 구조 작성 |
| G5.05 | 런타임 아키텍처 설계 | AA·SA | 정상·오류·Timeout·장애 흐름 작성 |
| G5.06 | 데이터 아키텍처 설계 | DA | 생성·저장·복제·조회·보관 구조 작성 |
| G5.07 | 연계 아키텍처 설계 | AA·DA | 동기·비동기·파일·배치 구조 작성 |
| G5.08 | 보안 아키텍처 설계 | SEC·AA | SSO·JWT·권한·암호화·감사 구조 작성 |
| G5.09 | 운영 아키텍처 설계 | OPS·TA·AA | 로그·모니터링·알림·장애 대응 구조 작성 |
| G5.10 | Deployment 대안 설계 | TA·AA | VM·JVM·Tomcat·WAR 배치대안 작성 |
| G5.11 | 주요 대안 비교·ADR 작성 | SA 공동 | Gateway, JWT, WAR, DR 등 선택근거 기록 |
| G5.12 | TO-BE Gap·전환 로드맵 작성 | SA | 신규·변경·폐기 항목과 순서 정의 |
| G5.13 | G5 Gate 심의 | SA | 목표 아키텍처·ADR 승인 |

G5 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| ARC-01 | 시스템 Context View | B | SA | 아키텍처위원회 |
| ARC-02 | 목표 아키텍처 Big Picture | B | SA | 프로젝트 책임자 |
| ARC-03 | 논리 아키텍처 정의서 | B | SA·AA | 아키텍처위원회 |
| ARC-04 | 애플리케이션 아키텍처 정의서 | B | AA | SA |
| ARC-05 | 런타임 아키텍처 정의서 | B | AA·SA | SA |
| ARC-06 | 데이터 아키텍처 정의서 | B | DA | SA |
| ARC-07 | 연계 아키텍처 정의서 | B | AA·DA | SA |
| ARC-08 | 보안 아키텍처 정의서 | B | SEC | 보안책임자·SA |
| ARC-09 | 운영 아키텍처 정의서 | B | OPS·TA·AA | SA |
| ARC-10 | Deployment View | B | TA·AA | SA |
| ARC-11 | 아키텍처 대안 비교서 | D | SA 공동 | 아키텍처위원회 |
| ARC-12 | ADR 목록·결정서 | R | SA | 아키텍처위원회 |
| ARC-13 | TO-BE Gap 및 전환 로드맵 | B | SA | 프로젝트 책임자 |
| ARC-14 | G5 Gate 결과서 | E | QA | SA |

11. G6 — TCF 플랫폼·개발표준 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G6.01 | TCF 플랫폼 책임·범위 정의 | AA·FW | TCF·STF·ETF·Dispatcher 책임 확정 |
| G6.02 | 표준 요청·응답 전문 설계 | FW·AA | Header·Body·Result 구조 확정 |
| G6.03 | 업무코드·ServiceId·거래코드 표준 | AA | 식별자 형식과 중복방지 기준 확정 |
| G6.04 | BASE·도메인·계층 패키지 표준 | AA·FW | 업무코드→도메인→계층 구조 확정 |
| G6.05 | Handler–Mapper 책임·호출 표준 | AA·FW | 허용·금지 호출과 트랜잭션 경계 확정 |
| G6.06 | STF 전처리 순서 설계 | FW | 인증·통제·Timeout·로그 시작순서 확정 |
| G6.07 | ETF 후처리·오류 표준 설계 | FW | 정상·업무오류·시스템오류 종료방식 확정 |
| G6.08 | 거래통제·Timeout·Idempotency 설계 | FW·AA | 정책 우선순위와 OM 관리범위 확정 |
| G6.09 | 예외·오류코드·메시지 표준 | FW·BA | 코드 영역·메시지·HTTP 상태 확정 |
| G6.10 | 거래로그·감사로그·추적ID 표준 | FW·OPS·SEC | GUID·TraceId·ServiceId 기록기준 확정 |
| G6.11 | OM Service Catalog·기준정보 설계 | FW·OPS | 등록항목·상태·변경·감사 구조 확정 |
| G6.12 | 명명규칙·코딩·SQL 표준 작성 | AA·DA | 클래스·DTO·Mapper·DB·파일 기준 확정 |
| G6.13 | 자동 구조검증·품질 Gate 설계 | FW·DVO·QA | ArchUnit·Checkstyle·등록검증 기준 확정 |
| G6.14 | 개발·코드리뷰 체크리스트 작성 | AA·QA | 개발완료 판정기준 확정 |
| G6.15 | G6 Gate 심의 | SA·AA | 개발표준 Baseline 승인 |

G6 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| STD-01 | TCF 처리 프레임워크 설계서 | B | FW·AA | SA |
| STD-02 | 표준 전문 설계서 | B | FW | AA |
| STD-03 | 업무코드·ServiceId·거래코드 기준 | B | AA | SA |
| STD-04 | BASE·패키지 구조 설계기준 | B | AA·FW | SA |
| STD-05 | 프로그램 6계층 설계기준 | B | AA | SA |
| STD-06 | STF·ETF 처리 설계서 | B | FW | AA |
| STD-07 | 거래통제 설계서 | B | FW·OPS | AA |
| STD-08 | Timeout 설계서 | B | FW·TA | AA |
| STD-09 | Idempotency·재처리 설계서 | B | FW·AA | SA |
| STD-10 | 예외·오류코드 설계서 | B | FW·BA | AA |
| STD-11 | 거래로그·감사로그 설계서 | B | FW·OPS·SEC | SA |
| STD-12 | OM 기준정보·Service Catalog 설계서 | B | FW·OPS | SA |
| STD-13 | 개발 명명규칙 종합서 | B | AA·DA | SA |
| STD-14 | SQL·DB 객체 설계기준 | B | DA·DBA | SA |
| STD-15 | 자동검증·품질 Gate 기준서 | B | FW·DVO·QA | SA |
| STD-16 | 개발·코드리뷰 체크리스트 | T | AA·QA | SA |
| STD-17 | G6 Gate 결과서 | E | QA | SA |

TCF는 표준 전문 검증, 인증 문맥, 거래통제, Timeout, 중복요청, ServiceId 라우팅, 표준 오류와 거래로그 종료를 일관되게 수행해야 한다.

12. G7 — 영역별 상세설계 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G7.01 | 화면·메뉴·기능권한 상세설계 | UI·BA·SEC | 화면 ID, 이벤트, 권한, 오류 처리 확정 |
| G7.02 | 화면 이벤트–ServiceId 매핑 | BA·AA | 모든 이벤트의 호출 거래 연결 |
| G7.03 | 거래설계서 작성 | BA·AA | 전문, 통제, Timeout, 흐름, 오류 정의 |
| G7.04 | 프로그램 설계서 작성 | DEV·AA | Handler 이하 클래스·메서드·DTO 정의 |
| G7.05 | 논리·물리 데이터 모델 설계 | DA | Entity·Table·Column·관계 확정 |
| G7.06 | SQL·Mapper·인덱스 설계 | DEV·DA·DBA | SQL ID, 실행계획, Timeout 확정 |
| G7.07 | 도메인 간 연동 상세설계 | AA | Contract·ServiceId·이벤트·Timeout 정의 |
| G7.08 | 외부 인터페이스 상세설계 | AA·DA | 전문·오류·SLA·재시도·보안 확정 |
| G7.09 | 인증·JWT·권한 상세설계 | SEC·AA | 발급·검증·Claim·권한·감사 확정 |
| G7.10 | Batch·Scheduler 상세설계 | AA·DA | Job·Step·재시작·Checkpoint 정의 |
| G7.11 | 파일·다운로드 상세설계 | AA·SEC | 크기·저장·권한·감사·정리 정의 |
| G7.12 | Cache 상세설계 | AA·DA | Key·TTL·무효화·현행성 정의 |
| G7.13 | 화면–거래–프로그램–SQL 추적성 작성 | QA·AA | 정방향·역방향 추적 가능 |
| G7.14 | 상세설계 Inspection | AA·DA·SEC | 계층·데이터·보안·운영 정합성 확인 |
| G7.15 | G7 Gate 심의 | SA 공동 | 개발 가능 수준 상세설계 승인 |

G7 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| DES-01 | 화면 설계서 | D | UI·BA | 업무책임자 |
| DES-02 | 메뉴·기능·데이터권한 설계서 | D | BA·SEC | SEC·업무책임자 |
| DES-03 | 화면 이벤트–ServiceId 매핑표 | R | BA·AA | AA |
| DES-04 | 거래설계서 | D | BA·AA | AA·업무책임자 |
| DES-05 | 프로그램 설계서 | D | DEV | AA |
| DES-06 | DTO·전문 필드 정의서 | D | DEV·BA | AA |
| DES-07 | 논리 데이터 모델 | D | DA | DA·업무책임자 |
| DES-08 | 물리 데이터 모델·테이블 정의서 | D | DA·DBA | DA |
| DES-09 | SQL·Mapper 설계서 | D | DEV·DBA | DA·AA |
| DES-10 | 인터페이스 설계서 | D | AA·DA | SA |
| DES-11 | 도메인 간 연동 설계서 | D | AA | SA |
| DES-12 | 인증·JWT·권한 상세설계서 | D | SEC·AA | SEC |
| DES-13 | Batch·Scheduler 설계서 | D | AA·DA | SA |
| DES-14 | 파일 업·다운로드 설계서 | D | AA·SEC | SA |
| DES-15 | Cache 설계서 | D | AA·DA | SA |
| DES-16 | 전체 추적성 매트릭스 | R | QA·AA | PMO·SA |
| DES-17 | 상세설계 Inspection 결과서 | E | QA | SA |
| DES-18 | G7 Gate 결과서 | E | QA | SA |

화면 이벤트와 업무 프로그램을 연결하는 핵심 식별자는 업무별 Controller가 아니라 ServiceId이며, 추적성은 화면 이벤트에서 Mapper·SQL·DB 객체까지 이어져야 한다.

13. G8 — 용량·물리구조·가용성·DR WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G8.01 | 사용자·세션·부하 조건 확정 | TA·BA | 평시·피크·스트레스 조건 승인 |
| G8.02 | TPS·동시 실행거래 산정 | TA·AA | Little’s Law 등 산정근거 기록 |
| G8.03 | 업무복잡도·CPU·메모리 모델링 | TA·AA | 거래유형별 부하 가중치 정의 |
| G8.04 | VM·Core·Memory 산정 | TA | 운영·장애·DR 필요용량 산출 |
| G8.05 | JVM Heap·GC·Metaspace 설계 | TA | JVM별 권장설정과 여유율 확정 |
| G8.06 | Tomcat Connector·Thread 설계 | TA·AA | maxThreads, Queue, Timeout 확정 |
| G8.07 | Hikari Pool·DB Session 산정 | AA·DA·DBA | WAR별 Pool과 DB 총수용량 검증 |
| G8.08 | WAR·Tomcat 업무그룹 배치설계 | TA·AA | 자원공유와 장애격리 수준 확정 |
| G8.09 | L4·Apache·Gateway 라우팅 설계 | TA·AA | Health Check와 Route 확정 |
| G8.10 | 가용성·장애격리 설계 | TA·SA | SPOF 제거와 잔여용량 검증 |
| G8.11 | 백업·복구·DR 설계 | TA·DA | RTO·RPO와 전환·원복 절차 설계 |
| G8.12 | 성능시험 부하모델 작성 | QA·TA·AA | 평시·피크·스트레스·장애 시나리오 확정 |
| G8.13 | 용량·물리구조 통합검토 | SA 공동 | 산정값과 실제 설정 연결 검증 |
| G8.14 | G8 Gate 심의 | SA | 용량·HA·DR 승인 |

G8 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| CAP-01 | 용량산정 전제조건서 | B | TA·BA | SA |
| CAP-02 | 사용자·동시성·TPS 산정서 | B | TA·AA | SA |
| CAP-03 | 업무복잡도·부하 가중치표 | D | AA·TA | SA |
| CAP-04 | VM·CPU·Memory 용량산정서 | B | TA | SA |
| CAP-05 | JVM·GC 설정 설계서 | D | TA | SA |
| CAP-06 | Tomcat Thread·Connector 설계서 | D | TA·AA | SA |
| CAP-07 | Hikari Pool·DB Session 산정서 | B | AA·DA·DBA | SA |
| CAP-08 | VM–JVM–Tomcat–WAR 배치표 | B | TA·AA | SA |
| CAP-09 | L4·Apache·Gateway 라우팅 설계서 | D | TA·AA | SA |
| CAP-10 | 가용성·장애격리 설계서 | B | TA·SA | 프로젝트 책임자 |
| CAP-11 | DR·백업·복구 설계서 | B | TA·DA | 프로젝트 책임자 |
| CAP-12 | 성능시험 부하모델 | D | QA·TA | SA |
| CAP-13 | 물리 아키텍처 구성도 | B | TA | SA |
| CAP-14 | G8 Gate 결과서 | E | QA | SA |

단일 Tomcat에 여러 WAR를 배포하면 Spring Context와 Pool은 WAR별로 분리할 수 있지만 JVM·Heap·GC·Connector Thread·프로세스 장애영역은 공유하므로, 용량과 장애격리를 함께 검토해야 한다.

14. G9 — 개발환경·CI/CD·배포기반 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G9.01 | Git 저장소·모듈구조 설계 | DVO·AA | 플랫폼·업무·설정 저장소 구분 |
| G9.02 | Branch·Merge·Release 정책 수립 | DVO·PMO | 승인·리뷰·Release Tag 기준 확정 |
| G9.03 | 개발자 로컬환경 표준화 | FW·DVO | JDK·Gradle·IDE·DB 실행절차 확정 |
| G9.04 | 환경별 Profile·설정 외부화 | DVO·TA | DEV~PROD 동일 소스 배포 가능 |
| G9.05 | Secret·Key 관리체계 구축 | SEC·DVO | Git·WAR·로그 평문 비밀정보 제거 |
| G9.06 | Gradle Build·WAR 생성 자동화 | DVO·FW | 재현 가능한 빌드 수행 |
| G9.07 | CI 정적분석·단위·구조검사 구축 | DVO·QA·FW | 실패 시 Merge 차단 |
| G9.08 | 보안·Dependency 검사 구축 | SEC·DVO | 취약점·라이선스 위반 검출 |
| G9.09 | 환경별 자동배포 Pipeline 구축 | DVO·TA | 배포·검증·승인 단계 자동화 |
| G9.10 | Health·Smoke Test 자동화 | QA·DVO | 배포 후 정상성 자동판정 |
| G9.11 | Rollback·버전관리 자동화 | DVO·TA | 이전 버전 원복 재현 가능 |
| G9.12 | 로그·모니터링·알림 기반 구축 | OPS·TA·AA | 인스턴스·WAR·ServiceId 식별 가능 |
| G9.13 | 개발·검증환경 준비도 점검 | QA | 신규 개발자 동일절차 실행 성공 |
| G9.14 | G9 Gate 심의 | SA·DVO | 개발·배포 기반 승인 |

G9 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| DVO-01 | 소스 저장소·모듈 구조서 | B | DVO·AA | SA |
| DVO-02 | Git Branch·Merge·Release 정책 | B | DVO | PMO·SA |
| DVO-03 | 로컬 개발환경 가이드 | O | FW·DVO | AA |
| DVO-04 | 환경구성·Profile 관리기준 | B | DVO·TA | SA |
| DVO-05 | Secret·Key 관리설계서 | B | SEC·DVO | 보안책임자 |
| DVO-06 | Gradle Build 표준 | T | FW·DVO | AA |
| DVO-07 | CI Pipeline 설계서 | D | DVO·QA | SA |
| DVO-08 | CD·배포 Pipeline 설계서 | D | DVO·TA | SA |
| DVO-09 | 배포·Rollback 절차서 | O | DVO·TA | OPS·SA |
| DVO-10 | Health·Smoke Test 명세 | E | QA·DVO | SA |
| DVO-11 | 로그·모니터링·알림 설계서 | D | OPS·TA·AA | SA |
| DVO-12 | 환경 준비도 점검결과서 | E | QA | SA |
| DVO-13 | G9 Gate 결과서 | E | QA | SA |

15. G10 — 선도개발 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G10.01 | 선도개발 대상 거래 선정 | AA·BA | 조회·변경·연계·배치 등 대표성 확보 |
| G10.02 | 선도 화면·거래·프로그램 설계 | AA·DEV | G7 표준을 적용한 상세설계 완료 |
| G10.03 | TCF 전체 처리구간 구현 | FW·DEV | Controller→TCF→STF→Handler→ETF 실행 |
| G10.04 | ServiceId·Handler·OM 등록검증 | FW·OPS | 코드와 Catalog 일치 |
| G10.05 | JWT·권한·감사 검증 | SEC·DEV | 인증 Claim과 Header 정합성 확인 |
| G10.06 | 거래통제·Timeout·중복요청 검증 | FW·QA | 차단·만료·중복 시나리오 성공 |
| G10.07 | 업무오류·시스템오류·Rollback 검증 | DEV·QA | 오류 분류와 트랜잭션 원복 확인 |
| G10.08 | WAR 간·외부연계 검증 | AA·DEV | 계약·TraceId·Timeout 전달 확인 |
| G10.09 | SQL·Pool·성능 측정 | TA·DBA·QA | SQL과 Pool 병목 근거 수집 |
| G10.10 | 배포·Health·Rollback 검증 | DVO·OPS | 자동배포와 원복 성공 |
| G10.11 | 운영로그·Runtime 진단 검증 | OPS·FW | ServiceId·SQL·자원 추적 성공 |
| G10.12 | 선도개발 Gap 분석 | SA 공동 | 구조·표준·도구 보완항목 도출 |
| G10.13 | 표준·템플릿 개정 | AA·FW | 본 개발 적용 기준 확정 |
| G10.14 | G10 Gate 심의 | SA | Critical Gap 0건, 본 개발 승인 |

G10 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| PIL-01 | 선도개발 대상 선정서 | B | AA·BA | SA |
| PIL-02 | 선도 화면·거래·프로그램 설계서 | D | AA·DEV | SA |
| PIL-03 | 선도개발 소스·표준 샘플 | E | FW·DEV | AA |
| PIL-04 | 선도개발 OM 등록정보 | E | FW·OPS | AA |
| PIL-05 | 선도개발 테스트 시나리오 | D | QA | AA |
| PIL-06 | 기능·오류·Timeout 검증결과 | E | QA | SA |
| PIL-07 | 성능·자원 측정결과 | E | TA·QA·DBA | SA |
| PIL-08 | 배포·Rollback 검증결과 | E | DVO·OPS | SA |
| PIL-09 | 운영로그·추적성 검증결과 | E | OPS·FW | SA |
| PIL-10 | 선도개발 Gap 분석서 | R | SA 공동 | SA |
| PIL-11 | 표준·템플릿 개정내역 | R | AA·FW | SA |
| PIL-12 | G10 Gate 결과서 | E | QA | SA |

선도개발은 정상 거래뿐 아니라 업무오류, 시스템오류, Timeout, Rollback, 로그·감사 추적, 배포·원복까지 검증한 뒤 본 개발에 진입해야 한다.

16. G11 — 본 개발·OM 등록·품질 Gate WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G11.01 | 업무별 개발계획·Iteration 구성 | PMO·DEV | 업무·도메인·Release 단위 계획 수립 |
| G11.02 | 화면·이벤트 구현 | UI | 설계서와 화면 이벤트 일치 |
| G11.03 | Handler–Mapper 프로그램 구현 | DEV | 계층·패키지·명명표준 준수 |
| G11.04 | DTO·Validation·예외 구현 | DEV | 표준 전문·오류체계 적용 |
| G11.05 | SQL·Mapper·DB 객체 구현 | DEV·DA·DBA | SQL 설계·Timeout·인덱스 검증 |
| G11.06 | 도메인·WAR 간 연계 구현 | DEV·AA | 계약·TraceId·Timeout 적용 |
| G11.07 | 권한·개인정보·감사 구현 | DEV·SEC | 기능·데이터 권한과 마스킹 적용 |
| G11.08 | OM Catalog·거래통제·Timeout 등록 | DEV·OPS | 코드·설계·OM 정합성 확보 |
| G11.09 | 단위·구조 테스트 작성 | DEV·FW | 커버리지와 ArchUnit 기준 충족 |
| G11.10 | 코드리뷰·SQL Inspection | AA·DA·DBA | Critical·Major 위반 조치 |
| G11.11 | CI 품질 Gate 수행 | DVO·QA | Build·Test·Security 통과 |
| G11.12 | 통합환경 배포·Smoke Test | DVO·QA | Release 후보 정상 배포 |
| G11.13 | 결함·Gap·기술부채 관리 | PMO·SA | 미조치항목 책임·기한 지정 |
| G11.14 | 개발완료 추적성 검증 | QA | 요구사항–코드–OM–시험 연결 |
| G11.15 | G11 Gate 심의 | SA·PMO | 개발완료·통합시험 진입 승인 |

G11 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| DEV-01 | 업무별 개발계획·Iteration 계획 | R | PMO·DEV | PMO |
| DEV-02 | UI·업무·프레임워크 소스 | E | UI·DEV·FW | AA |
| DEV-03 | Mapper XML·SQL·DB Script | E | DEV·DA | DA |
| DEV-04 | 환경설정·배포 Manifest | E | DVO·DEV | TA |
| DEV-05 | OM Service Catalog 등록대장 | R | DEV·OPS | AA |
| DEV-06 | 거래통제·Timeout 등록대장 | R | DEV·OPS | AA |
| DEV-07 | 단위테스트 코드·결과 | E | DEV | QA |
| DEV-08 | Architecture Test 결과 | E | FW·QA | AA |
| DEV-09 | 코드리뷰 결과서 | E | AA·DEV | AA |
| DEV-10 | SQL Inspection 결과서 | E | DA·DBA | DA |
| DEV-11 | CI 품질 Gate 결과 | E | DVO·QA | SA |
| DEV-12 | 통합 Build·WAR·Checksum 목록 | E | DVO | OPS |
| DEV-13 | 개발 결함·기술부채 대장 | R | PMO·SA | SA |
| DEV-14 | 개발완료 추적성 매트릭스 | R | QA | PMO·SA |
| DEV-15 | G11 Gate 결과서 | E | QA | SA |

17. G12 — 통합·성능·보안·장애·DR 시험 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G12.01 | 종합 시험전략·일정 수립 | QA·PMO | 시험범위·환경·Entry·Exit 기준 확정 |
| G12.02 | 시험 데이터·계정 준비 | QA·DA·SEC | 개인정보 보호된 대표 데이터 확보 |
| G12.03 | 기능·화면 통합시험 | QA·BA | 요구사항 수용기준 검증 |
| G12.04 | TCF 거래 통합시험 | QA·FW | STF·Dispatcher·ETF 전체 경로 검증 |
| G12.05 | 도메인·WAR·외부연계 계약시험 | QA·AA | 요청·응답·오류·버전·SLA 검증 |
| G12.06 | Batch·파일·대용량 시험 | QA·DA | 처리량·재시작·정합성 검증 |
| G12.07 | 성능·부하·스트레스 시험 | QA·TA | TPS·p95·자원·오류율 목표 검증 |
| G12.08 | 장시간·Memory·Pool 시험 | QA·TA | Leak·GC·Connection 반환 검증 |
| G12.09 | 보안·권한·취약점 시험 | SEC·QA | 인증·권한·개인정보·Secret 검증 |
| G12.10 | 장애·Failover·복구 시험 | TA·QA·OPS | JVM·DB·Network·연계 장애 대응 검증 |
| G12.11 | DR 전환·원복 시험 | TA·DA·OPS | RTO·RPO·데이터 정합성 검증 |
| G12.12 | 배포실패·Rollback 시험 | DVO·QA | 버전·DB·설정 원복 검증 |
| G12.13 | 로그·모니터링·알림 시험 | OPS·QA | 장애탐지와 ServiceId 추적 검증 |
| G12.14 | 결함 조치·회귀시험 | DEV·QA | Blocker·Critical 0건 |
| G12.15 | 시험결과 종합·잔여위험 평가 | QA·SA | 운영 전환 가능 여부 평가 |
| G12.16 | G12 Gate 심의 | SA 공동 | 운영 후보 Release 승인 |

G12 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| TST-01 | 종합 시험계획서 | B | QA | SA·PMO |
| TST-02 | 시험 시나리오·케이스 | D | QA·BA | 업무책임자 |
| TST-03 | 시험 데이터·계정 관리대장 | R | QA·DA·SEC | SEC |
| TST-04 | 기능·통합시험 결과서 | E | QA | 업무책임자 |
| TST-05 | 인터페이스·계약시험 결과서 | E | QA·AA | SA |
| TST-06 | Batch·파일·대용량 시험결과 | E | QA·DA | SA |
| TST-07 | 성능·부하·스트레스 시험결과 | E | QA·TA | SA |
| TST-08 | 장시간·JVM·Pool 시험결과 | E | QA·TA | SA |
| TST-09 | 보안·권한·취약점 시험결과 | E | SEC·QA | 보안책임자 |
| TST-10 | 장애·Failover·복구 시험결과 | E | TA·QA | SA |
| TST-11 | DR 전환·원복 시험결과 | E | TA·DA·OPS | 프로젝트 책임자 |
| TST-12 | 배포·Rollback 시험결과 | E | DVO·QA | OPS·SA |
| TST-13 | 로그·모니터링·알림 시험결과 | E | OPS·QA | SA |
| TST-14 | 결함·회귀시험 관리대장 | R | QA | PMO |
| TST-15 | 잔여위험 평가서 | B | SA 공동 | 프로젝트 책임자 |
| TST-16 | G12 Gate 결과서 | E | QA | SA |

18. G13 — 운영전환·Go-Live WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G13.01 | 운영 준비도 점검계획 수립 | OPS·PMO | 계정·권한·설정·Runbook 점검 |
| G13.02 | 운영 계정·권한·Secret 준비 | OPS·SEC·DBA | 최소권한과 승인·분리 적용 |
| G13.03 | 운영 환경설정·Route 확정 | TA·DVO·AA | L4·Apache·Gateway·WAR 설정 검증 |
| G13.04 | OM 기준정보 운영본 확정 | OPS·FW | Catalog·통제·Timeout·오류코드 확정 |
| G13.05 | 데이터 이행계획·리허설 | DA·DBA·QA | 건수·금액·코드·참조 정합성 검증 |
| G13.06 | 배포·Rollback 리허설 | DVO·OPS | 시간·담당·검증·원복 확인 |
| G13.07 | 장애·배포·백업 Runbook 작성 | OPS·TA·AA·DA | 상황별 조치와 담당조직 확정 |
| G13.08 | 운영자·Help Desk 교육 | OPS·SA | OM·로그·모니터링·장애판단 교육 |
| G13.09 | 비상연락망·War Room 체계 | PMO·OPS | 업무·개발·DB·인프라·보안 연락 확정 |
| G13.10 | Cutover 상세계획 작성 | PMO·DVO·DA | 시간대별 전환·검증·중단 기준 확정 |
| G13.11 | Go/No-Go 사전점검 | QA·SA | 필수 조치와 잔여위험 확인 |
| G13.12 | Go/No-Go 심의 | 프로젝트 책임자 | 운영전환 승인 또는 중지 |
| G13.13 | 운영 배포·데이터 이행 | DVO·DA·OPS | 승인 계획에 따른 전환 수행 |
| G13.14 | 핵심 거래·데이터 검증 | QA·BA·OPS | Smoke·정합성·권한 검증 |
| G13.15 | 트래픽 개방·집중관제 | OPS·TA·AA | 오류·성능·자원 상태 안정 |
| G13.16 | 전환결과 보고·인수 | PMO·OPS | 운영 인수와 잔여조치 등록 |

G13 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| OPS-01 | 운영 준비도 체크리스트 | O | OPS·QA | SA |
| OPS-02 | 운영 계정·권한 목록 | R | OPS·SEC | 보안책임자 |
| OPS-03 | 운영 환경설정 확정본 | B | DVO·TA | OPS·SA |
| OPS-04 | 운영 OM 기준정보 등록본 | B | OPS·FW | AA |
| OPS-05 | 데이터 이행계획서 | B | DA·DBA | 프로젝트 책임자 |
| OPS-06 | 데이터 이행 리허설 결과 | E | DA·QA | SA |
| OPS-07 | 배포·Rollback Runbook | O | DVO·OPS | SA |
| OPS-08 | 장애 대응 Runbook | O | OPS·TA·AA·DA | SA |
| OPS-09 | 백업·복구 Runbook | O | TA·DA·OPS | SA |
| OPS-10 | 운영자 교육자료·결과 | E | OPS·SA | PMO |
| OPS-11 | 비상연락망·War Room 운영표 | O | PMO·OPS | 프로젝트 책임자 |
| OPS-12 | Cutover 상세계획서 | B | PMO·DVO·DA | 프로젝트 책임자 |
| OPS-13 | Go/No-Go 체크리스트 | E | QA·SA | 프로젝트 책임자 |
| OPS-14 | Go/No-Go 승인서 | E | PMO | 프로젝트 책임자 |
| OPS-15 | 운영전환 수행결과서 | E | PMO·OPS | 프로젝트 책임자 |
| OPS-16 | 운영 인수인계서 | O | OPS | 운영책임자 |
| OPS-17 | G13 Gate 결과서 | E | QA | 프로젝트 책임자 |

운영전환 단계에서는 모니터링, 장애 Runbook, 배포·Rollback, 백업·복구, 운영자 교육, 비상연락망, Go-Live 판정을 함께 준비해야 한다.

19. G14 — 초기 안정화·지속 개선 WBS

| WBS | 작업 | 주관 | 주요 결과·완료기준 |
| --- | --- | --- | --- |
| G14.01 | 안정화 운영계획 수립 | OPS·PMO | 기간·조직·지표·종료기준 확정 |
| G14.02 | 일일 운영상태 점검 | OPS | 오류·Timeout·CPU·GC·Thread·Pool 확인 |
| G14.03 | Slow ServiceId·SQL 분석 | AA·DA·DBA | 병목 거래와 SQL 개선 |
| G14.04 | 사용자 문의·업무결함 대응 | BA·DEV·OPS | 우선순위별 조치와 회귀검증 |
| G14.05 | 장애 RCA·재발방지 관리 | SA 공동 | 원인·영향·조치·표준반영 |
| G14.06 | 용량산정 가정·임계치 보정 | TA·AA | 실제 부하 기반 설정 조정 |
| G14.07 | 잔여 Gap·기술부채 우선순위화 | SA·PMO | 운영 전 필수와 중장기 개선 구분 |
| G14.08 | 아키텍처·개발표준 개정 | SA·AA·FW | 운영 결과를 기준서에 반영 |
| G14.09 | 미사용 기능·Route·ServiceId 식별 | AA·OPS | 폐기 후보와 사용량 근거 확보 |
| G14.10 | 호환성·폐기계획 수립 | SA·AA·DA | 공지·병행·차단·제거 일정 정의 |
| G14.11 | 운영 KPI·SLA 기준선 확정 | OPS·PMO | 정상 운영 Baseline 확정 |
| G14.12 | 안정화 종료·프로젝트 종결 | PMO·SA | 인수완료·잔여과제 이관 |
| G14.13 | G14 Gate 심의 | 프로젝트 책임자 | 안정화 종료 승인 |

G14 단계 산출물

| ID | 산출물 | 등급 | 작성 | 승인 |
| --- | --- | --- | --- | --- |
| STA-01 | 초기 안정화 운영계획 | O | OPS·PMO | 프로젝트 책임자 |
| STA-02 | 일일·주간 안정화 보고서 | E | OPS | PMO·SA |
| STA-03 | 운영 성능·용량 Baseline | B | TA·AA | SA |
| STA-04 | Slow ServiceId·SQL 개선대장 | R | AA·DA·DBA | SA |
| STA-05 | 장애 RCA 보고서 | E | SA 공동 | 프로젝트 책임자 |
| STA-06 | 재발방지 Action Item 대장 | R | PMO·SA | 프로젝트 책임자 |
| STA-07 | 잔여 Gap·기술부채 대장 | R | SA 공동 | PMO |
| STA-08 | 아키텍처·개발표준 개정본 | B | SA·AA·FW | 아키텍처위원회 |
| STA-09 | 미사용 기능·Route·ServiceId 목록 | R | AA·OPS | SA |
| STA-10 | 변경·호환성·폐기계획서 | B | SA·AA·DA | 프로젝트 책임자 |
| STA-11 | 운영 KPI·SLA 기준서 | B | OPS·PMO | 운영책임자 |
| STA-12 | 운영 인수완료 확인서 | E | OPS | 프로젝트 책임자 |
| STA-13 | 프로젝트 종결·잔여과제 이관서 | E | PMO | 프로젝트 책임자 |
| STA-14 | G14 Gate 결과서 | E | QA | 프로젝트 책임자 |

20. 산출물 유형별 통합 목록

20.1 프로젝트 기준·거버넌스 산출물

- 아키텍처 관리계획서
- RACI
- Architecture Gate 정의서
- ADR 관리대장
- Risk·Gap·기술부채 대장
- 변경·Baseline 관리절차
- 아키텍처 예외 승인서
- 산출물 목록·템플릿
20.2 요구사항·업무 산출물

- 프로젝트 비전·목표서
- 범위도·대상·제외 업무 목록
- 정량적 KPI
- 업무기능 분해도
- 업무 용어사전
- 업무코드·도메인 정의서
- 도메인 Context Map
- 데이터 소유권 매트릭스
- 기능·비기능 요구사항
- 품질속성 시나리오
- RTM
20.3 아키텍처 산출물

- AS-IS·TO-BE 구성도
- Context View
- Big Picture
- 논리 아키텍처
- 애플리케이션 아키텍처
- 런타임 아키텍처
- 데이터 아키텍처
- 연계 아키텍처
- 보안 아키텍처
- 운영 아키텍처
- Deployment View
- ADR·대안 비교서
- Gap·전환 로드맵
20.4 TCF·개발표준 산출물

- TCF 처리 프레임워크 설계서
- 표준 전문 설계서
- ServiceId·거래코드 기준
- BASE·패키지 구조 기준
- 프로그램 6계층 기준
- STF·ETF 설계서
- 거래통제·Timeout·Idempotency 설계서
- 예외·오류코드 기준
- 거래로그·감사로그 기준
- OM Service Catalog 설계서
- 명명규칙 종합서
- SQL·DB 객체 기준
- 자동검증·품질 Gate 기준
- 코드리뷰 체크리스트
20.5 상세설계 산출물

- 화면설계서
- 메뉴·기능·데이터권한 설계서
- 화면 이벤트–ServiceId 매핑표
- 거래설계서
- 프로그램 설계서
- DTO·전문 필드 정의서
- 논리·물리 데이터 모델
- 테이블·컬럼·인덱스 정의서
- SQL·Mapper 설계서
- 인터페이스 설계서
- 도메인 연동 설계서
- 인증·JWT 상세설계서
- Batch·Scheduler 설계서
- 파일·Cache 설계서
- 전체 추적성 매트릭스
20.6 인프라·용량 산출물

- 용량산정 전제조건
- 사용자·동시성·TPS 산정서
- VM·CPU·Memory 산정서
- JVM·GC 설정서
- Tomcat Thread 설계서
- Hikari Pool·DB Session 산정서
- VM–JVM–Tomcat–WAR 배치표
- L4·Apache·Gateway Route 설계서
- 가용성·장애격리 설계서
- DR·백업·복구 설계서
- 성능시험 부하모델
20.7 개발·DevOps 산출물

- 저장소·모듈 구조
- Branch·Merge·Release 정책
- 로컬 개발환경 가이드
- Profile·환경설정 관리기준
- Secret·Key 관리설계
- Build·CI·CD Pipeline 설계
- Health·Smoke Test
- 배포·Rollback 절차
- 소스·SQL·환경설정
- OM 등록대장
- 단위·구조 테스트 결과
- 코드·SQL 리뷰 결과
- CI 품질 Gate 결과
20.8 시험·운영 산출물

- 종합 시험계획·시나리오
- 기능·통합·계약 시험결과
- 성능·장시간 시험결과
- 보안·권한 시험결과
- 장애·Failover·DR 시험결과
- 배포·Rollback 시험결과
- 운영 준비도 체크리스트
- Cutover 계획
- 장애·배포·복구 Runbook
- Go/No-Go 승인서
- 운영전환 결과
- 안정화 보고서
- RCA·재발방지 대장
- 운영 KPI·SLA 기준
- 프로젝트 종결·잔여과제 이관서
21. 산출물 간 핵심 추적관계

```
REQ-기능 요구사항
  ↓
DOM-업무기능·도메인
  ↓
DES-화면·이벤트
  ↓
DES-거래설계서
  ↓
STD-ServiceId·거래코드
  ↓
DES-프로그램 설계서
  ↓
DEV-Handler·Facade·Service·Rule·DAO·Mapper
  ↓
DES/DEV-SQL·DB 객체
  ↓
DEV-OM Catalog·Timeout·거래통제
  ↓
TST-테스트케이스·시험결과
  ↓
OPS-거래로그·감사로그·Runbook
```

21.1 필수 추적 키

| 추적영역 | 핵심 식별자 |
| --- | --- |
| 요구사항 | 요구사항 ID |
| 업무 | 업무기능 ID·유스케이스 ID |
| 화면 | 화면 ID·이벤트 ID |
| 거래 | ServiceId·거래코드 |
| 프로그램 | 클래스·메서드 |
| 데이터 | Mapper Namespace·SQL ID·테이블 |
| 테스트 | 테스트케이스 ID |
| 운영 | GUID·TraceId·배포버전 |
| 변경 | 변경요청 ID·ADR ID |

22. 단계별 Gate 통과기준 요약

| Gate | 필수 통과기준 |
| --- | --- |
| G0 | 책임·Gate·ADR·변경체계 승인, Blocker 0건 |
| G1 | 비전·범위·KPI 이해관계자 승인 |
| G2 | 현행 목록 완성, 기술부채·전환대상 식별 |
| G3 | 업무·도메인·데이터 소유권 승인 |
| G4 | 기능·비기능 요구사항 Baseline |
| G5 | 목표 Architecture View와 ADR 승인 |
| G6 | TCF·개발표준 Baseline |
| G7 | 개발 가능 수준 상세설계와 추적성 확보 |
| G8 | 용량·물리·HA·DR 근거 승인 |
| G9 | 재현 가능한 Build·Deploy·Rollback |
| G10 | 대표 거래 정상·오류·Timeout·Rollback 검증 |
| G11 | 코드·OM·시험·리뷰 정합성 확보 |
| G12 | 종합 시험 통과, Blocker·Critical 0건 |
| G13 | Runbook·Rollback·이행 준비 및 Go-Live 승인 |
| G14 | 안정화 종료조건 충족, 잔여과제 운영 이관 |

Gate 판정은 적합, 조건부 적합, 부적합, 재검토로 구분하며, 조건부 적합은 운영영향이 낮고 담당자·완료일·검증자가 지정된 보완계획이 있을 때만 허용한다.

23. WBS 관리 체크리스트

| No. | 점검항목 | 확인 |
| --- | --- | --- |
| 1 | 모든 WBS에 주관 책임자가 지정되어 있는가 | □ |
| 2 | 작업의 입력자료와 선행조건이 명확한가 | □ |
| 3 | 작업별 완료기준이 문서·코드 존재 여부 이상으로 정의되었는가 | □ |
| 4 | WBS와 산출물 ID가 연결되어 있는가 | □ |
| 5 | 산출물 작성자와 승인자가 분리되어 있는가 | □ |
| 6 | Baseline 대상과 지속 갱신 대상이 구분되어 있는가 | □ |
| 7 | 요구사항–설계–코드–OM–시험 추적이 가능한가 | □ |
| 8 | Architecture Gate 일정이 Master WBS에 포함되어 있는가 | □ |
| 9 | 조건부 적합 항목에 담당자·기한·검증자가 있는가 | □ |
| 10 | 아키텍처 예외에 만료일과 개선계획이 있는가 | □ |
| 11 | 개발보다 늦게 확정되는 필수 표준이 없는가 | □ |
| 12 | 용량산정이 실제 JVM·Thread·Pool 설정과 연결되는가 | □ |
| 13 | 운영 Runbook 작성이 운영전환 직전에 집중되지 않는가 | □ |
| 14 | 배포·Rollback·DR 시험이 WBS에 포함되어 있는가 | □ |
| 15 | 안정화 이후 표준 개정과 폐기관리까지 포함되어 있는가 | □ |

24. 시사점

24.1 핵심 아키텍처 판단

첫째, WBS의 최상위 구조는 조직별 작업이 아니라 Architecture Gate와 결과물 중심으로 구성해야 한다.

```
비권장:
업무팀 작업
개발팀 작업
인프라팀 작업
DB팀 작업

권장:
G3 업무·도메인 확정
G5 목표 아키텍처 승인
G6 TCF 표준 확정
G10 선도검증
G12 운영 가능성 검증
```

둘째, 설계와 개발은 완전히 분리된 일회성 단계가 아니다. 다만 핵심 경계와 표준이 확정되기 전에 본 개발을 시작해서는 안 된다.

셋째, 선도개발은 단순 샘플 프로그램이 아니라 전체 아키텍처를 검증하는 공식 Gate다.

넷째, OM 등록은 개발 후 운영팀이 수행하는 사후작업이 아니다.

```
ServiceId 설계
→ Handler 구현
→ OM Catalog 등록
→ 거래통제·Timeout 등록
→ 통합시험
```

하나의 개발 완료조건으로 관리해야 한다.

다섯째, 운영 산출물은 G13에서 처음 작성하는 것이 아니라 G5 운영 아키텍처, G6 로그·OM 표준, G9 모니터링 기반, G10 선도검증 단계부터 점진적으로 완성해야 한다.

24.2 주요 위험

| 위험 | 결과 |
| --- | --- |
| G3 이전 WAR·패키지 확정 | 업무경계와 배포경계 불일치 |
| G4 NFR 미확정 | 용량·DR·성능시험 기준 부재 |
| G5 ADR 미작성 | 기술결정 근거와 책임 불명확 |
| G6 표준 지연 | 업무팀별 임의 구현 확산 |
| G10 선도개발 생략 | 본 개발 후 구조결함 발견 |
| OM 등록 분리 | 실행되지 않는 ServiceId 발생 |
| 시험을 기능 중심으로 제한 | 성능·장애·복구 위험 미발견 |
| Runbook 사후작성 | 실제 운영절차 검증 불가 |
| 안정화 종료조건 부재 | 프로젝트가 장기 미종결 상태로 잔존 |

24.3 우선 적용 과제

- G0~G14를 프로젝트 Master WBS의 Level 1로 등록한다.
- 본 문서의 WBS 코드를 Level 2·3 작업으로 등록한다.
- 모든 WBS 작업에 대응 산출물 ID를 연결한다.
- Gate 일정을 주요 Milestone으로 등록한다.
- G0·G1·G3·G4·G5·G6을 본 개발 전 필수 선행 Gate로 지정한다.
- G10 선도개발 통과를 본 개발 확대의 조건으로 지정한다.
- 산출물 승인과 RTM 등록을 작업 완료율 100% 조건으로 사용한다.
- G13 운영전환 이전에 G12 Blocker·Critical 0건을 강제한다.
25. 마무리말

NSIGHT WBS는 다음 세 가지를 동시에 관리해야 한다.

```
무엇을 수행하는가
누가 책임지는가
어떤 증적으로 완료를 판단하는가
```

최종적인 프로젝트 관리 흐름은 다음과 같다.

```
WBS 작업 등록
→ 담당자 지정
→ 입력자료 확보
→ 작업 수행
→ 산출물 작성
→ 자체·통합검토
→ 결함 조치
→ Gate 심의
→ Baseline
→ 다음 단계 진입
```

이 구조를 적용하면 프로젝트 진척률을 단순한 문서 개수나 코드 작성량으로 판단하지 않고, 아키텍처 의사결정과 운영 가능성이 실제로 확보되었는지를 기준으로 판단할 수 있다.

