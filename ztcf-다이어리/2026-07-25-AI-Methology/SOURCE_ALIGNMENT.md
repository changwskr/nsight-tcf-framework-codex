# 기준 소스 정합성 검토

본 MVP의 생성 템플릿은 업로드된 `소스.zip`의 대표 구현을 대조하여 작성했습니다.

## 확인한 대표 구조

```text
sv-service
  ├─ entry/handler/SvCustomerHandler.java
  ├─ entry/facade/SvCustomerFacade.java
  ├─ application/service/SvCustomerService.java
  ├─ application/rule/SvCustomerRule.java
  ├─ application/dto/customer/*
  ├─ persistence/dao/SvCustomerDao.java
  ├─ persistence/dto/customer/CustomerSummaryRow.java
  ├─ persistence/mapper/SvCustomerMapper.java
  └─ resources/mapper/sv/SvCustomerMapper.xml
```

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

현재 기준 소스는 `entry/application/persistence` 계층형 구조입니다. 기존 설계 문서에는 `업무코드 → 도메인 → 계층` 형태의 목표 구조도 정의되어 있습니다. 도구는 두 구조를 모두 지원합니다.

- `CURRENT_SOURCE`: 기존 소스와 즉시 병합하기 위한 기본값
- `DOMAIN_FIRST`: 신규 업무·구조개편을 위한 목표형

## 생성 후 반드시 보완할 항목

- 상세 업무 규칙과 오류코드
- 트랜잭션 범위와 Rollback 정책
- 실제 RDW/ADW SQL, Join, Hint, 인덱스
- 개인정보·금액·권한·감사 기준
- Timeout 예산분해
- OM 실제 테이블·API 스키마
- 실제 NSIGHT 전체 Gradle Build와 통합시험
