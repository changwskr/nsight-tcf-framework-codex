# 업무 거래 설계 가이드

## 설계 목표

업무 거래는 공통 TCF 파이프라인을 재사용하고 업무 코드만 명확한 계층에 배치해야 한다. 하나의 변경이 어떤 `serviceId`, Handler, 트랜잭션과 데이터에 영향을 주는지 추적 가능해야 한다.

## 권장 구성

```text
{business}-service/src/main/
├─ java/com/nh/nsight/marketing/{business}/
│  ├─ entry/handler
│  ├─ entry/facade
│  ├─ application/dto
│  ├─ application/service
│  ├─ application/rule
│  ├─ client
│  ├─ persistence/dao
│  ├─ persistence/dto
│  ├─ persistence/mapper
│  └─ config, support
└─ resources/
   ├─ mapper/{business}
   ├─ application.yml
   ├─ application-local.yml
   ├─ application-dev.yml
   └─ application-prod.yml
```

## 구현 절차

1. Business Code, Domain과 action을 결정한다.
2. 중복되지 않는 `serviceId`를 정의한다.
3. 요청, 응답, 검색 조건과 DB Row DTO를 구분한다.
4. 도메인 Handler의 `serviceIds()`와 `switch` 분기를 추가한다.
5. Facade에서 요청 DTO 변환과 트랜잭션 경계를 설정한다.
6. Service에서 Rule, DAO와 Client 호출 순서를 구성한다.
7. Rule에 입력 검증과 업무 불변조건을 둔다.
8. DAO/Mapper와 Mapper XML을 구현한다.
9. 성공, 결과 없음, 잘못된 입력, 업무 오류와 타임아웃을 테스트한다.
10. Service Catalog, 샘플 요청과 관련 문서를 갱신한다.

## 계층별 금지사항

| 계층 | 피해야 할 구현 |
|---|---|
| Handler | SQL 실행, 복잡한 업무 규칙, 트랜잭션 직접 관리 |
| Facade | 대규모 SQL 조립, 공통 TCF 정책 재구현 |
| Service | HTTP 요청 객체 의존, 웹 응답 직접 생성 |
| Rule | DB와 외부 시스템에 대한 숨은 부작용 |
| DAO/Mapper | 업무 상태 판단, 표준 응답 생성 |

## 트랜잭션과 타임아웃

- 읽기 거래는 `@Transactional(readOnly = true)`를 우선 검토한다.
- 쓰기 거래는 유스케이스 단위로 원자성을 정의한다.
- 코드에 고정 타임아웃을 추가하기 전에 서비스 타임아웃 정책과의 관계를 확인한다.
- 온라인 타임아웃은 Dispatcher 실행 경계를 제한한다.
- 트랜잭션 타임아웃은 DB 작업의 트랜잭션 경계를 제한한다.
- MyBatis 쿼리 타임아웃은 개별 SQL 실행을 제한한다.

## 오류 처리

- 예상 가능한 업무 실패는 `BusinessException`과 정의된 Error Code를 사용한다.
- 예상하지 못한 예외는 임의로 성공 응답으로 변환하지 않는다.
- 내부 예외 메시지와 스택 추적을 클라이언트에 그대로 노출하지 않는다.
- 신규 오류 코드는 기존 코드 체계 및 OM Error Catalog와 정합성을 맞춘다.

## 리뷰 체크리스트

- [ ] `serviceId` 형식과 중복 여부를 확인했다.
- [ ] Handler가 도메인당 하나라는 원칙을 지켰다.
- [ ] DTO와 영속성 Row 객체가 분리되어 있다.
- [ ] 트랜잭션과 타임아웃 경계가 명시되어 있다.
- [ ] 로그에 민감정보가 포함되지 않는다.
- [ ] 단위 및 통합 테스트가 오류 경로를 포함한다.
- [ ] 관련 카탈로그, 샘플 요청과 문서를 갱신했다.

