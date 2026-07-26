# 2단계 — 역공학 문서화 프롬프트 (AV 완성본)

> 원본: `2026-07-26-nsight-tcf-prompts.md` 8번 (역공학 — 코드 → 설계서).
> 대상: 이미 구현 완료된 av-service (AV.Sample.inquiry, 포트 8101,
> tcf-ui 화면 sample-list.html 포함) — 역공학의 최적 실전 대상입니다.
> 다른 모듈(sv·ep·ic 등)에 쓸 때는 모듈명·경로만 교체하세요.

```text
너는 NSIGHT-TCF-FRAMEWORK 방법론 문서화 담당자야.

대상 모듈: av-service (업무코드 AV — 구현 완료, 설계서 없음)

먼저 대상 모듈의 소스를 계층별로 스캔해서 수집해:
- av-service/src/main/java/com/nh/nsight/marketing/av/
  · entry/handler/AvSampleHandler — serviceIds() 등록 목록
  · entry/facade/AvSampleFacade — @Transactional 속성(readOnly·timeout)
  · application/service/AvSampleService — 처리 흐름(이중 조회)
  · application/rule/AvSampleRule — 검증·보정 규칙(페이징 기본값·상한)
  · application/dto/sample/ · persistence/dto/sample/ — DTO 필드
  · persistence/dao/AvSampleDao · persistence/mapper/AvSampleMapper
- av-service/src/main/resources/
  · mapper/av/AvSampleMapper.xml — SQL(동적 WHERE·OFFSET/FETCH·count)
  · schema.sql — AV_SAMPLE 정의·시드
  · application.yml · application-local.yml — 포트(8101)·business-code·
    거래로그 datasource 설정값
- tcf-ui 연동분:
  · static/av/sample-list.html — 필터 필드 ID·그리드 컬럼·이벤트
  · _shared/av-admin.js — TX 정의(serviceId·transactionCode)·릴레이 호출
  · support/BusinessModuleDefinitions.java 의 AV 등록(8101)
  · sample-requests/av-sample-inquiry.json

그 다음 ztcf-methodology/EB-프로그램-설계서.md 의 9개 섹션 형식에
'정확히 맞춰' 다음 2개 문서를 생성해:
1. ztcf-methodology/AV-프로그램-설계서.md
   (① 시스템 개요 ② 레이어 설계 ③ 거래 목록 ④ 클래스 명세 4.1~4.6
    ⑤ DTO 설계 ⑥ 테이블 설계 ⑦ 배치 설계(해당 없음이면 명시)
    ⑧ 오류 처리 ⑨ 환경 설정 — 모든 값은 소스에서 확인한 실제 값만 기재)
2. ztcf-methodology/AV-UI-레이아웃-설계서.md
   (EB-UI-레이아웃-설계서.md 6개 섹션 형식 — sample-list.html의
    실제 필드 ID·컬럼·이벤트를 표로)

작성 규칙:
- 소스에 없는 내용을 추측해서 쓰지 마 — 모든 표의 값은 실제 코드에서
  확인한 것만 기재하고, 확인 못한 항목은 "확인 불가"로 표시해
- §4.5 SQL 요약은 Mapper XML의 실제 구문을 요약해 (임의 각색 금지)

마지막으로 "설계 품질 소견" 섹션을 두 문서 끝에 붙여서
EB 표준 패턴과 다르게 구현된 부분(계층 생략, 명명 불일치,
트랜잭션 속성 누락, ServiceId 규칙 위반 등)을 지적하고
보정 방법을 제안해줘. 차이가 없으면 "표준 일치"로 명시해.

완료 보고: 생성한 문서 2개의 경로와,
설계서 §3 거래 목록 ↔ 실제 serviceIds() 등록 목록의 대조표를 제출해.
```
