# NSIGHT-TCF-FRAMEWORK 맞춤 프롬프팅 메시지 모음

> 원본 기법: [2026-07-26-exam.md](2026-07-26-exam.md)
> 목적: "앞으로 무엇을 더 개발해야 하는가"를 AI가 직접 근거를 수집해 결정하도록 하는 실행형 프롬프트

---

## 1. ReAct — 미구현 영역 정량 분석 후 개발 우선순위 결정

```text
지금부터 NSIGHT-TCF-FRAMEWORK의 미구현 영역을 분석해야 해.

먼저, 표준 레퍼런스인 eb-service의 패키지 구조(entry/handler, entry/facade,
application/service, application/rule, persistence/dao, persistence/mapper)와
클래스 목록을 로드하고,

그 다음, 스켈레톤 상태인 6개 업무 모듈(mg·ms·pc·pd·ss·om-service, 각 209 LOC)을
eb-service와 클래스 단위로 비교하는 분석을 실행해서
모듈별 "누락 클래스 수 / 누락 계층 / 미등록 serviceId"를 표로 집계해.

추가로 소스 전체에서 TODO·FIXME 주석과
tcf-core의 미완성 지점(빈 구현, UnsupportedOperationException)을 검색해.

이 결과를 바탕으로 개발 우선순위 TOP 5를 선정하고,
각 항목의 예상 작업량(참조: eb-service 2,447 LOC 기준)과
선행 의존성(tcf-core → tcf-web → 업무 서비스 순서)을 보고해줘.
```

## 2. RAG — 설계 문서 근거 기반 로드맵 제안

```text
답변하기 전에,
먼저 ztcf-methodology/의 EB-프로그램-설계서.md와 EB-UI-레이아웃-설계서.md,
zdocs-1/architecture/의 아키텍처 정의서(architecture.md),
그리고 ztcf-다이어리/2026-07-25 전체 파일 목차/의 개발자 핵심지식 문서를 검색해.

또한 README.md의 "프레임워크 역량" 표에 선언된 7개 영역
(거래통제·Timeout·거래로그·세션·Cache·오류코드·ServiceId)이
실제 코드(tcf-core, tcf-web, tcf-om)에 어디까지 구현됐는지 대조해.

그리고 네가 찾은 그 자료들에 '근거해서'
설계 문서에는 있지만 코드에 없는 기능 갭 목록을 만들고,
NSIGHT-TCF-FRAMEWORK가 다음 분기에 추가해야 할 기능 로드맵을 제안해.
각 제안마다 근거 문서의 파일 경로와 해당 섹션 제목을 출처로 명시해줘.
```

## 3. 자기성찰식 답변 요구 — 아키텍처 분석 보고서 검증

```text
이제 네가 방금 작성한 NSIGHT-TCF-FRAMEWORK 아키텍처 분석 보고서
(모듈러 모놀리스, 프레임워크 89% vs 업무 11%, Handler 중심 파이프라인 결론)를
'객관적인 아키텍처 리뷰어'의 입장에서 다시 읽어봐.

'가장 치명적인 논리적 약점'이나
'수치가 결론을 뒷받침하지 못하는 부분'이
어디라고 생각하는지 정확히 지적해.

예를 들어 — LOC 비율만으로 "프레임워크 중심"이라 결론내린 것이 타당한가?
tcf-om·tcf-oc의 13,000 LOC가 관리 화면 CRUD라면 그 해석이 달라지지 않는가?
스켈레톤 6개 모듈을 "교육용"으로 단정한 근거는 충분한가?

그리고 그 문제점을 해결하기 위해 필요한 추가 검증
(예: 클래스별 역할 분류, 복잡도 측정)을 직접 수행한 뒤,
네가 '스스로 개선'한 최종 보고서를 나에게 제출해.
```

## 4. 메타프롬프팅 — 신규 업무 모듈 개발 프롬프트 생성기

```text
너는 세계 최고의 프롬프트 엔지니어이자
NSIGHT-TCF-FRAMEWORK 아키텍처 전문가야.

내가 새로운 업무 모듈(예: av-service)을
이 프레임워크의 표준 패턴대로 개발하려 할 때 사용할,
가장 완벽한 '개발 지시 프롬프트'를 만들어줘.

프롬프트에는 반드시 다음이 포함되어야 해:
- 6계층 패키지 규약 (entry/handler → facade → service → rule → dao/mapper)
- ServiceId 형식 ({업무코드}.{업무명}.{처리유형})
- NsightWarBootstrap 상속, settings.gradle·build.gradle 등록 절차
- 레퍼런스로 삼을 파일 경로 (eb-service 기준)
- 완료 검증 방법 (bootRun 포트, POST /{업무코드}/online 샘플 호출)

내가 반드시 제공해야 할 정보들
(업무코드, 도메인 엔티티, 거래 목록, DB 테이블 등)이 있다면
프롬프트를 만들기 전에 나에게 먼저 질문해줘.
```

## 5. ReAct + RAG 결합 — 운영 리스크 기반 보강 항목 도출 (추천)

```text
지금부터 NSIGHT-TCF-FRAMEWORK의 운영 리스크를 분석해야 해.

먼저, 다음 데이터를 수집해서 수치로 로드해:
1. 테스트 커버리지 실태 — 모듈별 src/test 파일 수와
   본문 코드 대비 비율 (테스트 없는 모듈 목록 포함)
2. 보안 설정 실태 — 하드코딩된 계정·비밀번호·시크릿 검색
   (admin01/nsight01! 같은 값이 코드·설정·문서 몇 곳에 노출되는지)
3. 단일 장애점 — 공유 H2(TCP 9092) 의존 모듈 수와
   H2 장애 시 영향받는 기능 목록
4. 프로덕션 준비도 — tcf-cicd의 dev/prod 프로파일에서
   local과 달라지는 설정 항목 수

그 다음, zdocs-1 설계 문서에서 각 항목의 원래 설계 의도를 검색해서
"설계 의도 vs 현재 구현" 갭을 확인하고,

이 결과를 바탕으로 프로덕션 전환 전 반드시 보강해야 할
항목 TOP 5를 위험도(High/Medium/Low)와 함께 보고해줘.
각 항목마다 근거 수치와 관련 파일 경로를 명시해.
```

---

# 개발 방법론 적용 프롬프트 (절차 + 템플릿)

> 기준 방법론: `ztcf-methodology/EB-프로그램-설계서.md` (9개 섹션) ·
> `EB-UI-레이아웃-설계서.md` (6개 섹션) — eb-service를 표준 레퍼런스로 사용

## 6. 템플릿 생성 — 설계서 2종을 재사용 가능한 표준 템플릿으로 추출

```text
너는 NSIGHT-TCF-FRAMEWORK 방법론 관리자야.

먼저 ztcf-methodology/EB-프로그램-설계서.md 와
EB-UI-레이아웃-설계서.md 를 읽고 문서 구조를 분석해.

그 다음 EB 고유 내용(EB_USER, Outbox, 화면 19410 등)을 제거하고
{업무코드}·{도메인}·{테이블} 플레이스홀더로 치환한
표준 템플릿 2종을 ztcf-methodology/ 에 생성해줘:

1. _템플릿-프로그램-설계서.md — 9개 섹션 유지:
   ① 시스템 개요 ② 아키텍처(레이어) 설계 ③ 거래(serviceId) 목록
   ④ 프로그램(클래스) 명세 (Handler/Facade/Service/Rule/DAO·Mapper/Client·Scheduler·Config)
   ⑤ DTO(전문) 설계 (요청/응답/Row) ⑥ 테이블 설계
   ⑦ 배치 설계 ⑧ 오류 처리 설계 ⑨ 환경 설정 요약

2. _템플릿-UI-레이아웃-설계서.md — 6개 섹션 유지:
   ① 문서 개요 ② 화면 목록 ③ 공통 레이아웃 구조
   ④ 화면별 상세 설계 (구성 블록/입력 필드/그리드 컬럼/이벤트 정의)
   ⑤ 화면-거래 매핑 요약 ⑥ 공통 UI 규칙

각 표의 컬럼 구성은 EB 원본과 동일하게 유지하고,
셀에는 작성 예시를 주석(예: "예) EB.User.create")으로 남겨.
마지막에 "작성 체크리스트" 섹션을 추가해서
설계서 완성도를 검증할 수 있는 항목(거래마다 Handler·Facade·화면 매핑 존재 여부 등)을 넣어줘.
```

## 7. 방법론 절차 실행 — 설계서 → 코드 단계별 구현 (설계 우선 개발)

```text
너는 NSIGHT-TCF-FRAMEWORK 방법론에 따라 새 업무 모듈을 구현하는 개발자야.
입력: {업무코드}-프로그램-설계서.md 와 {업무코드}-UI-레이아웃-설계서.md
(없으면 먼저 6번 템플릿으로 작성하라고 나에게 요청해.)

아래 절차를 Phase 단위로 진행하고, 각 Phase 완료 시
검증 게이트를 통과했는지 보고한 뒤 다음 Phase로 넘어가:

[Phase 0] 모듈 등록
- settings.gradle include, build.gradle(eb-service 복사·업무코드 치환),
  NsightWarBootstrap 상속 메인 클래스, application.yml(포트·business-code)
- 게이트: gradle :{모듈}:compileJava 성공

[Phase 1] 설계서 §6 테이블 → schema.sql 작성, §5 DTO → Request/Response/Row 클래스
- 게이트: bootRun 기동 시 schema 초기화 로그 확인

[Phase 2] 설계서 §4 명세 → persistence 계층 (Mapper 인터페이스 + XML + DAO)
- 게이트: 매퍼 XML의 SQL이 설계서 §4.5 "SQL 요약"과 일치

[Phase 3] 설계서 §4 명세 → application 계층 (Rule → Service 순서)
- Rule의 검증·보정 규칙은 설계서 §4.4 표를 그대로 구현
- 게이트: Rule 위반 시 BusinessException(BUSINESS_ERROR) 발생 확인

[Phase 4] 설계서 §3 거래 목록 → entry 계층 (Facade → Handler)
- serviceId switch 분기, 미지원 시 SERVICE_NOT_FOUND
- 게이트: POST /{업무코드}/online 으로 설계서 §3의 모든 serviceId 호출 성공

[Phase 5] UI 설계서 §4 → tcf-ui/static/{업무코드}/ 화면 구현
- 입력 필드 ID·그리드 컬럼·이벤트 정의를 설계서 표와 1:1 일치시켜
- 게이트: 화면에서 거래 호출 → 그리드 렌더링 확인

[Phase 6] 마감
- OM_SERVICE_CATALOG 등록, 거래로그 기록 확인, README 작성
- 게이트: 설계서 대비 구현 커버리지 100% 매트릭스 제출

절대 규칙: 설계서에 없는 클래스·거래를 임의로 추가하지 말고,
설계서가 모호하면 구현 전에 나에게 질문해.
```

## 8. 역공학 — 기존 코드 → 설계서 자동 생성 (문서화 절차)

```text
너는 NSIGHT-TCF-FRAMEWORK 방법론 문서화 담당자야.

대상 모듈: {모듈명} (예: sv-service — 1,097 LOC 구현됨, 설계서 없음)

먼저 대상 모듈의 소스를 계층별로 스캔해서 수집해:
- entry/handler의 TransactionHandler와 serviceIds() 등록 목록
- entry/facade의 @Transactional 속성(readOnly·timeout)
- application/service·rule의 메서드별 처리 로직과 검증 규칙
- persistence의 Mapper XML SQL과 Row DTO
- resources의 schema.sql·application.yml 설정값
- tcf-ui/static/{업무코드}/ 화면이 있으면 필드·그리드·이벤트

그 다음 EB-프로그램-설계서.md 의 9개 섹션 형식에 '정확히 맞춰'
ztcf-methodology/{업무코드}-프로그램-설계서.md 를 생성하고,
화면이 있으면 {업무코드}-UI-레이아웃-설계서.md 도 생성해.

마지막으로 "설계 품질 소견" 섹션을 붙여서
EB 표준 패턴과 다르게 구현된 부분(계층 생략, 명명 불일치,
트랜잭션 속성 누락 등)을 지적하고 보정 방법을 제안해줘.
```

---

# tcf-ai-methodology (Model Studio) 전용 프롬프트

> 대상: `tcf-ai-methodology` — 업무모델 정의·검증·코드생성 도구 (bootRun :8787)
> 참조 문서: `tcf-ai-methodology/README.md` · `docs/SOURCE_ALIGNMENT.md` ·
> `docs/DOMAIN_MODEL_INVENTORY.md` · `static/guide/ai-methodology.md` (20단계 절차)

## 9. ReAct — 생성기 품질 검증 (Model Studio 산출물 vs 수작업 표준 비교)

```text
지금부터 tcf-ai-methodology의 코드 생성 품질을 검증해야 해.

먼저, 생성기 소스를 로드해서 산출물 목록을 수치화해:
- generator/ 패키지 4개 클래스(DomainArtifactGenerator, DtoArtifactGenerator,
  DocArtifactGenerator, WorkspaceGenerator)가 만드는 파일 유형과 개수
- validation/ModelValidator의 검증 규칙 개수와 항목

그 다음, 시드 모델 중 EB 도메인 1건을 골라 생성 로직을 추적하고
(가능하면 bootRun :8787 기동 후 POST /api/preview 실행),
생성된 Handler·Facade·Service·Rule·DAO·Mapper XML을
수작업 표준인 eb-service의 실제 클래스와 항목별로 비교해:
- 계층별 클래스 구조·어노테이션·트랜잭션 속성 일치 여부
- 설계서(EB-프로그램-설계서.md) 9개 섹션 중 생성기가 커버 못하는 영역
  (예: Client 연계, Scheduler, UI HTML, 배치 설계)

이 결과를 바탕으로 "생성기가 만들 수 있는 것 vs 없는 것" 매트릭스와
생성기 템플릿에 추가해야 할 기능 TOP 3를 보고해줘.
```

## 10. 도구 활용 파이프라인 — Model Studio로 신규 모듈 개발 (7번 절차와 연동)

```text
너는 NSIGHT-TCF-FRAMEWORK 개발자야. 이번에는 수작업이 아니라
tcf-ai-methodology(Model Studio)를 활용해 신규 업무 모듈을 개발해.

입력: 업무코드 {BC}, 도메인 {Domain}, 거래 목록 {ServiceId들}
(없으면 나에게 먼저 질문해.)

절차:
[1] 모델 정의 — src/main/resources/data/sample_model.json 형식에 맞춰
    businessCode·domainCode·ServiceId·필드·검증규칙을 담은 모델 JSON 작성
    (핵심 규칙: 같은 businessCode+domainCode의 여러 ServiceId는 Handler 1개로 병합)
[2] 검증 — POST /api/validate 로 모델 검증, ValidationIssue 전부 해소
[3] 생성 — POST /api/generate 로 ZIP 산출, 내용물 목록 확인
    (Handler~Mapper XML, DTO, DDL, OM Service Catalog SQL, .http 샘플,
     화면·거래 정의서, 추적성 CSV, Quality Gate, manifest)
[4] 반영 — 생성물은 '초안'이므로 그대로 붙이지 말고:
    - eb-service 표준 패턴과 Diff 비교 후 차이 나는 부분 보정
    - 7번 프롬프트의 Phase 0~6 게이트를 그대로 통과시켜 검증
    - 생성 Rule은 골격이므로 실제 업무규칙을 보완
[5] 마감 — OM Service Catalog SQL 적용, .http 샘플로 전 거래 호출 검증,
    수정한 부분을 모델 JSON에 역반영(모델-코드 정합성 유지)

각 단계 완료 시 산출물 경로와 검증 결과를 보고하고,
Model Studio가 처리 못해서 수작업한 항목을 별도로 기록해줘.
```

## 11. RAG + 자기성찰 — 두 방법론 통합과 Model Studio 개선 로드맵

```text
답변하기 전에, 먼저 다음 자료를 모두 검색해서 로드해:
1. ztcf-methodology/ 의 설계서 기반 수동 방법론
   (EB-프로그램-설계서·EB-UI-레이아웃-설계서 — 9+6 섹션 체계)
2. tcf-ai-methodology/static/guide/ai-methodology.md 의 20단계(0~19) 자동화 절차와
   docs/NSIGHT_Automated_Development_Methodology.md 본문
3. docs/DOMAIN_MODEL_INVENTORY.md 시드 41건과
   실제 구현된 TransactionHandler 56개의 serviceId 대조표

그리고 네가 찾은 그 자료들에 '근거해서' 다음을 분석해:
- 두 방법론(설계서 수동 vs Model Studio 자동)이 겹치는 단계와 어긋나는 단계
- 시드 41건에 없는데 실제 코드에는 있는 serviceId (모델 인벤토리 갭)
- 설계서 템플릿(6번 프롬프트 산출물)을 Model Studio가 자동 생성하려면
  DocArtifactGenerator에 무엇을 추가해야 하는지

분석을 마친 뒤, '객관적인 비평가' 입장에서 네 통합안을 다시 읽고
가장 치명적인 약점(예: 모델 JSON이 표현 못하는 설계 정보)을 지적한 다음,
스스로 개선한 최종 "방법론 통합 + Model Studio 개선 로드맵"을
각 근거 자료의 출처와 함께 제출해줘.
```

---

## 사용 가이드

| # | 기법 | 답을 주는 질문 | 소요 |
|---|------|----------------|------|
| 1 | ReAct | 어떤 코드가 비어 있고 무엇부터 만들까? | 중 |
| 2 | RAG | 설계 문서 대비 무엇이 빠졌나? | 중 |
| 3 | 자기성찰 | 기존 분석 결론을 믿어도 되나? | 소 |
| 4 | 메타프롬프팅 | 새 모듈을 어떻게 시키면 정확히 만들까? | 소 |
| 5 | ReAct+RAG | 프로덕션 가려면 뭘 먼저 고쳐야 하나? | 대 |
| 6 | 템플릿 추출 | 설계서를 어떤 양식으로 쓰나? | 소 |
| 7 | 절차 실행 | 설계서대로 어떻게 구현하나? (Phase 0~6) | 대 |
| 8 | 역공학 | 이미 만든 코드를 어떻게 문서화하나? | 중 |
| 9 | ReAct | Model Studio 생성물을 믿어도 되나? | 중 |
| 10 | 파이프라인 | Model Studio로 어떻게 모듈을 찍어내나? | 대 |
| 11 | RAG+자기성찰 | 수동·자동 방법론을 어떻게 합치나? | 대 |

- **처음 실행 추천 순서**: 5번(리스크) → 1번(우선순위) → 4번(실행 프롬프트 생성)
- **방법론 적용 추천 순서**: 6번(템플릿 추출) → 설계서 작성 → 7번(Phase 절차 구현)
  - 이미 코드가 있는 모듈(sv·ep·ic 등)은 8번(역공학)으로 설계서부터 확보 후 7번으로 보완
- **Model Studio 활용 순서**: 9번(생성기 신뢰도 검증) → 10번(파이프라인 실전 적용) → 11번(방법론 통합·도구 개선)
- 각 프롬프트는 이 워크스페이스의 실제 경로·수치(2026-07-25 분석 기준)를 포함하고 있어
  Cursor 채팅에 그대로 붙여넣으면 AI가 즉시 도구로 검증을 수행할 수 있음
