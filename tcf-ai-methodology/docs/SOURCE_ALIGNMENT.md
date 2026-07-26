# 기준 소스 정합성 검토

본 도구의 생성 템플릿은 **nsight-tcf-framework** 저장소의 대표 구현을 대조하여 작성·유지합니다.  
(초기 Python MVP는 업로드 `소스.zip` 기준으로 작성되었으며, Spring Boot 모듈은 동일 계약을 repo 소스로 재확인합니다.)

## 확인한 대표 구조

```text
sv-service
  ├─ entry/handler/SvCustomerHandler.java      # SV.Customer.selectSummary
  ├─ entry/facade/SvCustomerFacade.java
  ├─ application/service/SvCustomerService.java
  ├─ application/rule/SvCustomerRule.java
  ├─ application/dto/customer/*
  ├─ persistence/dao/SvCustomerDao.java
  ├─ persistence/dto/customer/CustomerSummaryRow.java
  ├─ persistence/mapper/SvCustomerMapper.java
  └─ resources/mapper/sv/SvCustomerMapper.xml
```

시드 생성(`generate-domain-models.js`)은 추가로 다음을 스캔합니다.

| 출처 | 내용 |
|------|------|
| `*-service` / `tcf-om` `*Handler.java` | ServiceId 상수 |
| 각 모듈 `schema.sql` | 테이블·컬럼 |
| `sv-service` Customer DTO/Row | 필드 타입·마스킹 샘플 |

## 반영한 계약

- Handler는 `TransactionHandler`를 구현합니다.
- 도메인 Handler는 `serviceIds()`로 여러 ServiceId를 등록할 수 있습니다.
- Handler의 `doHandle()`은 `TransactionContext.header.serviceId`로 Facade Method를 분기합니다.
- Facade는 `@Transactional` 경계와 Request DTO 변환을 담당합니다.
- Service는 Rule·DAO를 조합하고 Response DTO를 반환합니다.
- Rule은 필수값·길이·조회 결과·처리 건수를 검증합니다.
- DAO는 Mapper Interface Method를 호출합니다.
- Mapper XML namespace는 Java Mapper FQCN과 일치합니다.
- Mapper Statement ID는 DAO/Mapper Method와 동일하게 생성합니다.
- MyBatis Statement Timeout을 생성합니다.

## 패키지 프로파일 판단

현재 기준 소스는 `entry / application / persistence` 계층형 구조입니다.  
설계 문서에는 `업무코드 → 도메인 → 계층` 형태의 목표 구조도 정의되어 있습니다.  
도구는 두 구조를 모두 지원합니다.

| 프로파일 | 용도 |
|----------|------|
| `CURRENT_SOURCE` | 기존 소스와 즉시 병합 (기본값) |
| `DOMAIN_FIRST` | 신규 업무·구조개편 목표형 |

## 시드와의 관계

- 시드 41건 = 위 Handler·schema에서 확인된 ServiceId 중심
- Sample-only WAR(PC/MS/PD/SS/MG)도 `*.Sample.inquiry` 스캐폴드를 포함
- 상세 목록: [DOMAIN_MODEL_INVENTORY.md](DOMAIN_MODEL_INVENTORY.md)

## 생성 후 반드시 보완할 항목

- 상세 업무 규칙과 오류코드
- 트랜잭션 범위와 Rollback 정책
- 실제 RDW/ADW SQL, Join, Hint, 인덱스
- 개인정보·금액·권한·감사 기준
- Timeout 예산분해
- OM 실제 테이블·API 스키마
- 실제 NSIGHT 전체 Gradle Build와 통합시험
