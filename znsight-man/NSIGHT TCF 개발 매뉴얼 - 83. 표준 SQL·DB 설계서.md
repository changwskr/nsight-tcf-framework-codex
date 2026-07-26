NSIGHT 표준 SQL·DB 설계서

SV.Customer.selectSummary — 고객 종합정보 조회

1. 도입 전 안내말

NSIGHT의 SQL·DB 설계서는 테이블 목록이나 SQL 문장만 기록하는 문서가 아니다.

하나의 화면 이벤트와 ServiceId가 어떤 DAO·Mapper SQL을 호출하고, 어떤 테이블과 컬럼을 조회하며, 해당 SQL이 어떤 인덱스와 실행계획을 사용하고, 장애 발생 시 어떤 화면과 프로그램이 영향을 받는지를 정의하는 데이터 실행 설계서다.

```
화면 이벤트
  ↓
ServiceId
  ↓
Handler → Facade → Service
  ↓
DAO Method
  ↓
Mapper Interface
  ↓
Mapper XML / SQL ID
  ↓
Table / View
  ↓
Column / PK / FK / Index / Constraint
  ↓
실행계획 / 성능 / 모니터링
```

NSIGHT TCF에서는 DB 객체와 프로그램이 다음 관계를 유지해야 한다.

```
ServiceId
↔ DAO Method
↔ Mapper Namespace
↔ SQL ID
↔ Table·View
↔ Column·Index
↔ 화면·테스트·운영로그
```

SQL 또는 테이블이 변경되면 DB 객체 → SQL ID → Mapper → DAO → Service → ServiceId → 화면 이벤트 순서로 역추적할 수 있어야 한다.

DB 객체명은 업무코드와 데이터 성격을 식별할 수 있어야 하며, 대문자·언더스코어·업무 Prefix·표준 약어를 적용한다.

2. 문서 개요

2.1 목적

| 목적 | 설명 |
| --- | --- |
| SQL 식별 | 거래에 사용되는 Mapper SQL과 역할 정의 |
| 데이터 모델 | 테이블·컬럼·관계·키 구조 정의 |
| 추적성 | ServiceId에서 DB 객체까지 정방향·역방향 연결 |
| 성능 확보 | 인덱스·실행계획·조회 건수·Timeout 기준 정의 |
| 정합성 확보 | PK·FK·Unique·Check·Not Null 제약 정의 |
| 보안 강화 | 개인정보 최소조회·마스킹·권한 기준 정의 |
| 운영성 확보 | Slow SQL과 오류를 ServiceId 기준으로 추적 |
| 품질 검증 | SQL 정적검증·실행계획·통합시험 기준 정의 |
| 변경관리 | DDL·SQL 변경과 호환성·Rollback 절차 정의 |

2.2 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 화면 | SV-CUS-0001 고객 종합정보 조회 |
| 이벤트 | SV-CUS-0001-E01 고객요약 조회 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 업무 WAR | sv-service |
| DAO | 고객요약·등급·상품 DAO |
| Mapper | 고객요약·등급·상품 Mapper |
| DBMS | Oracle 계열 RDB 기준 |
| 처리유형 | 온라인 동기 조회 |
| 데이터 | 고객요약·고객등급·보유상품 |
| 성능목표 | 거래 p95 3초 이내 |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 업무 개발자 | DAO·Mapper·SQL 구현 |
| DA | 논리모델과 데이터 표준 검토 |
| DBA | 물리모델·인덱스·통계·배포 검토 |
| 아키텍트 | 거래와 데이터 구조 정합성 검토 |
| 보안 담당자 | 개인정보와 접근통제 검토 |
| 테스트 담당자 | SQL·성능·장애 시험 작성 |
| 운영 담당자 | Slow SQL과 장애 영향 추적 |
| 품질 담당자 | 산출물·소스·DB 정합성 검증 |

2.4 선행조건

- ServiceId와 거래코드가 확정되어 있어야 한다.
- 요청·응답·Query·Result DTO가 정의되어 있어야 한다.
- 고객 식별키와 기준일 정책이 정의되어 있어야 한다.
- 고객요약·등급·상품의 원천과 적재주기가 확정되어 있어야 한다.
- 개인정보 분류와 보존기간이 승인되어 있어야 한다.
- 예상 데이터 건수와 증가량이 산정되어 있어야 한다.
- 운영 DB의 Schema·Tablespace·계정 정책이 정의되어 있어야 한다.
- 성능 목표와 SQL Timeout이 확정되어 있어야 한다.
2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| 논리명 | 업무에서 사용하는 데이터 명칭 |
| 물리명 | DB에 생성되는 영문 객체명 |
| SQL ID | MyBatis XML Statement 식별자 |
| Namespace | Mapper Interface와 XML을 연결하는 전체 이름 |
| Query DTO | SQL 입력조건을 담는 객체 |
| Result DTO | SQL 결과를 담는 객체 |
| Snapshot | 특정 기준일의 데이터 상태 |
| Cardinality | SQL 단계별 예상 처리 건수 |
| Selectivity | 조건으로 전체 데이터가 줄어드는 비율 |
| Covering Index | 필요한 컬럼을 인덱스에서 대부분 해결하는 구조 |
| Execution Plan | DB Optimizer가 선택한 SQL 실행 경로 |
| Bind Variable | SQL 재사용을 위해 값을 바인딩하는 변수 |
| Full Table Scan | 테이블 전체 블록을 읽는 실행 방식 |

3. 문제 정의 및 설계 배경

3.1 문제 정의

고객 종합정보 조회는 단순 단건 조회처럼 보이지만 다음 데이터가 결합된다.

- 고객 기본정보
- 고객 관리지점
- 마케팅 동의
- 고객등급
- 보유상품
- 데이터 기준일
SQL·DB 설계가 불명확하면 다음 문제가 발생한다.

| 문제 | 영향 |
| --- | --- |
| 하나의 거대 SQL로 모든 정보 조인 | 중복 행·실행계획 불안정 |
| 고객번호·고객명 조건을 OR로 결합 | 인덱스 사용 저하 |
| SELECT * 사용 | 불필요한 개인정보와 I/O 증가 |
| 인덱스 근거 부족 | Full Scan과 응답 지연 |
| 기준일 정책 불명확 | 서로 다른 시점 데이터 결합 |
| 상품 목록 무제한 조회 | 응답 크기와 메모리 증가 |
| FK·Unique 미정의 | 중복·고아 데이터 발생 |
| SQL Parameter 로그 출력 | 개인정보 유출 |
| 운영계 직접 DDL | Lock·장애·Rollback 어려움 |
| SQL 변경 추적성 부족 | 영향 화면·거래 식별 불가 |

3.2 설계 배경

본 거래는 다음 SQL을 분리하여 실행하는 것을 기본으로 한다.

```
고객번호 조회
  → selectCustomerSummaryByNo

고객명·지점 조회
  → selectCustomerSummaryByName

고객등급 조회
  → selectCustomerGrade

보유상품 조회
  → selectCustomerProducts
```

검색조건별 SQL을 분리하는 이유는 하나의 동적 SQL에 다음 조건을 넣지 않기 위해서다.

```
WHERE (:customerNo IS NULL OR CUSTOMER_NO = :customerNo)
  AND (:customerName IS NULL OR CUSTOMER_NAME LIKE :customerName)
```

이와 같은 선택적 OR 조건은 데이터 분포와 Bind 값에 따라 실행계획이 불안정해질 수 있다.

4. 현행 구조와 문제점

4.1 비권장 구조

```
Service
  ↓
하나의 Mapper SQL
  ↓
고객·등급·상품을 대규모 JOIN
  ↓
화면 DTO로 직접 반환
```

문제점

| 문제 | 설명 |
| --- | --- |
| 행 증폭 | 고객 1건 × 상품 N건으로 고객정보 반복 |
| Result 매핑 복잡 | 중첩 컬렉션 매핑과 중복 제거 필요 |
| 장애 범위 확대 | 상품 SQL 문제로 기본정보도 실패 |
| SQL 튜닝 어려움 | 조인 수와 조건 증가 |
| 재사용 저하 | 등급·상품 조회를 독립 사용하기 어려움 |
| 실행계획 불안정 | 통계·데이터 분포 변화에 민감 |

4.2 목표 구조

```
SvCustomerService
  ├─ SvCustomerDao.selectSummary()
  │    └─ 고객 1건
  ├─ SvCustomerGradeDao.selectGrade()
  │    └─ 등급 0~1건
  └─ SvCustomerProductDao.selectProducts()
       └─ 상품 0~100건
```

핵심 조회를 분리하되, 무분별하게 수십 개 SQL로 쪼개는 것도 금지한다.

5. 요구사항과 제약조건

5.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| DB-01 | 고객번호 기준 고객요약 1건을 조회해야 한다. |
| DB-02 | 고객명과 지점 조건으로 고객을 조회할 수 있어야 한다. |
| DB-03 | 기준일에 유효한 고객등급을 조회해야 한다. |
| DB-04 | 조회 가능한 보유상품을 최대 건수 내에서 반환해야 한다. |
| DB-05 | 고객 미존재는 0건으로 반환해야 한다. |
| DB-06 | 개인정보 원문은 필요한 컬럼만 조회해야 한다. |
| DB-07 | 데이터 기준시각을 응답할 수 있어야 한다. |
| DB-08 | 삭제·비활성 고객의 표시정책을 반영해야 한다. |

5.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 고객요약 SQL | 정상 300ms 이내 목표 |
| 고객등급 SQL | 정상 200ms 이내 목표 |
| 상품목록 SQL | 정상 300ms 이내 목표 |
| 전체 DB 처리 | 정상 1초 이내 목표 |
| SQL Timeout | 최대 3초 |
| 상품 최대 건수 | 100건 |
| 개인정보 | 최소 컬럼 조회 |
| 실행계획 | 운영 유사 데이터로 사전 검증 |
| 가용성 | SQL 오류 시 Connection 반환 보장 |
| 추적성 | ServiceId·Mapper·SQL ID 연결 |

5.3 제약조건

- SELECT *를 사용하지 않는다.
- MyBatis ${} 문자열 치환을 사용하지 않는다.
- 컬럼에 함수를 적용하여 인덱스를 무효화하지 않는다.
- 화면 정렬값을 검증 없이 SQL에 삽입하지 않는다.
- 온라인 조회에서 무제한 결과를 허용하지 않는다.
- Mapper에서 업무 권한을 하드코딩하지 않는다.
- 업무 오류코드를 SQL에서 결정하지 않는다.
- SQL 원문·Parameter·고객번호를 일반 운영로그에 기록하지 않는다.
- DDL은 승인된 배포도구와 변경절차로 적용한다.
- 운영 데이터에 대한 임의 수동 수정은 금지한다.
6. 설계 원칙

6.1 데이터 접근 책임 분리

```
Service
= 어떤 데이터를 어떤 순서로 사용할지 결정

DAO
= Mapper 호출과 데이터 접근 예외 처리

Mapper
= SQL 실행

DB
= 데이터 정합성과 물리 저장 보장
```

6.2 검색 경로 분리

| 검색 방식 | SQL ID | 주요 인덱스 |
| --- | --- | --- |
| 고객번호 조회 | selectCustomerSummaryByNo | 고객번호+기준일 |
| 고객명·지점 조회 | selectCustomerSummaryByName | 지점+정규화 고객명+기준일 |
| 고객등급 조회 | selectCustomerGrade | 고객번호+평가일 |
| 보유상품 조회 | selectCustomerProducts | 고객번호+상태+가입일 |

6.3 데이터 기준일 일관성

고객요약, 등급, 상품은 가능한 한 동일한 업무 기준일을 사용한다.

```
request.baseDate
  ↓
고객요약 BASE_DATE
고객등급 EVALUATION_DATE ≤ BASE_DATE
상품 SNAPSHOT_DATE = BASE_DATE
```

데이터 원천별 적재시점이 다르면 응답에 영역별 기준시각을 포함하거나 데이터 지연을 명시한다.

6.4 Bind Variable 원칙

```
WHERE CUSTOMER_NO = #{customerNo}
```

다음은 금지한다.

```
WHERE CUSTOMER_NO = '${customerNo}'
```

6.5 결과 건수 제한

상품 조회는 최대 100건을 반환한다.

101건을 조회하여 초과 여부를 판단한 후, 화면에는 100건만 반환할 수 있다.

```
FETCH FIRST 101 ROWS ONLY
```

7. 대안 비교 및 의사결정

7.1 데이터 조회 대안

| 대안 | 설명 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- | --- |
| A | 고객·등급·상품 단일 JOIN | DB 왕복 감소 | 행 증폭·매핑 복잡 | 비권장 |
| B | 영역별 3개 SQL 순차 실행 | 구조·튜닝 명확 | DB 왕복 증가 | 권장 |
| C | 영역별 SQL 병렬 실행 | 응답시간 단축 가능 | Connection 동시 사용 증가 | 제한 |
| D | 고객요약 전용 View·MV | 조회 단순화 | 적재·갱신 복잡 | 데이터량에 따라 |

7.2 의사결정

기본안은 영역별 3개 SQL 순차 실행으로 한다.

병렬 SQL은 한 거래가 복수 Connection을 점유하거나 동일 트랜잭션 문맥을 분리할 수 있으므로 기본 적용하지 않는다.

고객요약 조회량이 매우 높고 원천 조인이 복잡하다면 별도 Snapshot Table 또는 Materialized View를 검토한다.

8. 데이터 모델 개요

8.1 논리 관계

```
고객
SV_CUSTOMER_SUMMARY
  │ 1
  │
  ├──── 0..N 고객등급 이력
  │     SV_CUSTOMER_GRADE
  │
  └──── 0..N 고객상품 Snapshot
        SV_CUSTOMER_PRODUCT
```

8.2 핵심 관계

| 부모 객체 | 자식 객체 | 관계 | 기준 |
| --- | --- | --- | --- |
| 고객요약 | 고객등급 | 1:N | 고객번호 |
| 고객요약 | 고객상품 | 1:N | 고객번호·기준일 |
| 공통코드 | 고객유형 | 1:N | 코드그룹·코드 |
| 지점기준 | 관리지점 | 1:N | 지점코드 |

8.3 물리 객체 목록

| 객체 유형 | 객체명 | 용도 |
| --- | --- | --- |
| Table | SV_CUSTOMER_SUMMARY | 기준일별 고객요약 |
| Table | SV_CUSTOMER_GRADE | 고객등급 평가이력 |
| Table | SV_CUSTOMER_PRODUCT | 고객 보유상품 Snapshot |
| Index | PK_SV_CUSTOMER_SUMMARY | 고객요약 PK |
| Index | IX_SV_CUST_SUM_01 | 고객번호 조회 |
| Index | IX_SV_CUST_SUM_02 | 지점·고객명 조회 |
| Index | IX_SV_CUST_GRADE_01 | 최신 등급 조회 |
| Index | IX_SV_CUST_PROD_01 | 고객 상품 조회 |

DB 객체명은 대문자와 언더스코어를 사용하고 업무 테이블은 업무코드 Prefix로 시작한다.

9. 테이블 설계 — SV_CUSTOMER_SUMMARY

9.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 논리명 | 고객요약 Snapshot |
| 물리명 | SV_CUSTOMER_SUMMARY |
| Schema | SV_APP 예시 |
| 테이블 유형 | 업무 Snapshot |
| 주 식별자 | 기준일+고객번호 |
| 예상 CRUD | 조회 중심, 배치 적재 |
| 보존기간 | 업무·개인정보 정책에 따름 |
| 파티션 후보 | BASE_DATE |

9.2 컬럼 정의

| No. | 논리명 | 물리명 | 자료형 예시 | Null | 키 | 설명 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 기준일 | BASE_DATE | DATE | N | PK1 | Snapshot 기준일 |
| 2 | 고객번호 | CUSTOMER_NO | VARCHAR2(20) | N | PK2 | 내부 고객 식별자 |
| 3 | 고객명 | CUSTOMER_NAME | VARCHAR2(100) | N |  | 암호화·접근통제 대상 |
| 4 | 검색용 고객명 | CUSTOMER_NAME_NORM | VARCHAR2(100) | Y |  | 정규화 검색값 |
| 5 | 고객유형코드 | CUSTOMER_TYPE_CODE | VARCHAR2(10) | N |  | 개인·법인 등 |
| 6 | 관리지점코드 | MANAGEMENT_BRANCH_ID | VARCHAR2(10) | N |  | 데이터권한 기준 |
| 7 | 휴대전화번호 | MOBILE_NO | VARCHAR2(30) | Y |  | 개인정보 |
| 8 | 이메일 | EMAIL_ADDRESS | VARCHAR2(200) | Y |  | 개인정보 |
| 9 | 마케팅동의여부 | MARKETING_CONSENT_YN | CHAR(1) | N |  | Y/N |
| 10 | 고객상태코드 | CUSTOMER_STATUS_CODE | VARCHAR2(10) | N |  | 정상·해지 등 |
| 11 | 데이터기준시각 | DATA_AS_OF_DTM | TIMESTAMP(6) | N |  | 원천 데이터 기준 |
| 12 | 등록일시 | CREATED_DTM | TIMESTAMP(6) | N |  | 생성시각 |
| 13 | 수정일시 | UPDATED_DTM | TIMESTAMP(6) | N |  | 최종변경시각 |

9.3 키와 제약조건

| 제약조건 | 컬럼 | 기준 |
| --- | --- | --- |
| PK | BASE_DATE, CUSTOMER_NO | 기준일별 고객 유일 |
| NOT NULL | 필수 업무 컬럼 | Null 금지 |
| CHECK | MARKETING_CONSENT_YN | Y, N만 허용 |
| CHECK | 날짜 관계 | 데이터 기준시각 유효 |
| FK | 관리지점 | 물리 FK 여부 DBA 협의 |
| FK | 고객유형코드 | 대용량 Snapshot은 논리 FK 가능 |

9.4 인덱스

| 인덱스 | 컬럼 | 목적 |
| --- | --- | --- |
| PK_SV_CUSTOMER_SUMMARY | BASE_DATE, CUSTOMER_NO | PK |
| IX_SV_CUST_SUM_01 | CUSTOMER_NO, BASE_DATE | 고객번호 최신 기준일 조회 |
| IX_SV_CUST_SUM_02 | MANAGEMENT_BRANCH_ID, CUSTOMER_NAME_NORM, BASE_DATE | 지점+고객명 검색 |
| IX_SV_CUST_SUM_03 | BASE_DATE, CUSTOMER_STATUS_CODE | 배치·통계 후보 |

IX_SV_CUST_SUM_03은 실제 사용 SQL이 없다면 생성하지 않는다. 인덱스는 “향후 사용할 수도 있음”을 이유로 추가하지 않는다.

10. 테이블 설계 — SV_CUSTOMER_GRADE

10.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 논리명 | 고객등급 이력 |
| 물리명 | SV_CUSTOMER_GRADE |
| 주 식별자 | 고객번호+평가일+등급유형 |
| 데이터 특성 | 이력성 |
| 주요 조회 | 기준일 이하 최신 등급 |

10.2 컬럼 정의

| No. | 논리명 | 물리명 | 자료형 | Null | 키 |
| --- | --- | --- | --- | --- | --- |
| 1 | 고객번호 | CUSTOMER_NO | VARCHAR2(20) | N | PK1 |
| 2 | 등급유형코드 | GRADE_TYPE_CODE | VARCHAR2(10) | N | PK2 |
| 3 | 평가일 | EVALUATION_DATE | DATE | N | PK3 |
| 4 | 등급코드 | GRADE_CODE | VARCHAR2(20) | N |  |
| 5 | 등급점수 | GRADE_SCORE | NUMBER(12,4) | Y |  |
| 6 | 유효종료일 | VALID_TO_DATE | DATE | Y |  |
| 7 | 데이터기준시각 | DATA_AS_OF_DTM | TIMESTAMP(6) | N |  |
| 8 | 등록일시 | CREATED_DTM | TIMESTAMP(6) | N |  |

10.3 인덱스

| 인덱스 | 컬럼 | 목적 |
| --- | --- | --- |
| PK_SV_CUSTOMER_GRADE | CUSTOMER_NO, GRADE_TYPE_CODE, EVALUATION_DATE | PK |
| IX_SV_CUST_GRADE_01 | CUSTOMER_NO, GRADE_TYPE_CODE, EVALUATION_DATE DESC | 최신 등급 조회 |
| IX_SV_CUST_GRADE_02 | EVALUATION_DATE, GRADE_CODE | 배치·통계 필요 시 |

10.4 최신 등급 판단

기준일 이하의 가장 최근 평가일 한 건을 선택한다.

```
EVALUATION_DATE ≤ :baseDate
ORDER BY EVALUATION_DATE DESC
FETCH FIRST 1 ROW ONLY
```

동일 평가일에 복수 데이터가 발생하지 않도록 PK 또는 Unique 제약을 적용한다.

11. 테이블 설계 — SV_CUSTOMER_PRODUCT

11.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 논리명 | 고객 보유상품 Snapshot |
| 물리명 | SV_CUSTOMER_PRODUCT |
| 주 식별자 | 기준일+고객번호+고객상품ID |
| 주요 조회 | 고객별 정상·유효 상품 |
| 최대 반환 | 100건 |

11.2 컬럼 정의

| No. | 논리명 | 물리명 | 자료형 | Null | 키 |
| --- | --- | --- | --- | --- | --- |
| 1 | 기준일 | SNAPSHOT_DATE | DATE | N | PK1 |
| 2 | 고객번호 | CUSTOMER_NO | VARCHAR2(20) | N | PK2 |
| 3 | 고객상품ID | CUSTOMER_PRODUCT_ID | VARCHAR2(40) | N | PK3 |
| 4 | 상품코드 | PRODUCT_CODE | VARCHAR2(20) | N |  |
| 5 | 상품명 | PRODUCT_NAME | VARCHAR2(200) | N |  |
| 6 | 가입일 | JOIN_DATE | DATE | Y |  |
| 7 | 만기일 | MATURITY_DATE | DATE | Y |  |
| 8 | 상품상태코드 | PRODUCT_STATUS_CODE | VARCHAR2(10) | N |  |
| 9 | 잔액 | BALANCE_AMOUNT | NUMBER(19,2) | Y |  |
| 10 | 통화코드 | CURRENCY_CODE | CHAR(3) | Y |  |
| 11 | 데이터기준시각 | DATA_AS_OF_DTM | TIMESTAMP(6) | N |  |
| 12 | 등록일시 | CREATED_DTM | TIMESTAMP(6) | N |  |

11.3 인덱스

| 인덱스 | 컬럼 | 목적 |
| --- | --- | --- |
| PK_SV_CUSTOMER_PRODUCT | SNAPSHOT_DATE, CUSTOMER_NO, CUSTOMER_PRODUCT_ID | PK |
| IX_SV_CUST_PROD_01 | CUSTOMER_NO, SNAPSHOT_DATE, PRODUCT_STATUS_CODE | 고객 상품 조회 |
| IX_SV_CUST_PROD_02 | PRODUCT_CODE, SNAPSHOT_DATE | 상품 통계 필요 시 |

12. 파티션 설계

12.1 파티션 적용 판단

Snapshot 테이블은 기준일이 계속 증가하므로 BASE_DATE 또는 SNAPSHOT_DATE 기준 Range Partition 후보가 된다.

| 조건 | 판단 |
| --- | --- |
| 데이터가 수백만 건 이하 | 비파티션도 가능 |
| 일별 대량 Snapshot 누적 | 월 단위 Range Partition 권장 |
| 보존기간별 삭제 필요 | Partition Drop 활용 |
| 최근일자 조회 집중 | Partition Pruning 효과 |
| 고객번호 조회만 대부분 | 로컬·글로벌 인덱스 검토 필요 |

12.2 권장 예시

```
PARTITION BY RANGE (BASE_DATE)
INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
```

실제 적용 여부는 다음을 확인한 뒤 결정한다.

- 월 증가 건수
- 보존기간
- 배치 적재 방식
- 고객번호 조회 비율
- 글로벌 인덱스 유지비용
- 백업·복구 전략
파티션은 데이터량 근거 없이 형식적으로 적용하지 않는다.

13. SQL–프로그램–DB 매핑

| ServiceId | DAO Method | Mapper Namespace | SQL ID | 대상 객체 |
| --- | --- | --- | --- | --- |
| SV.Customer.selectSummary | selectSummary() | SvCustomerMapper | selectCustomerSummaryByNo | SV_CUSTOMER_SUMMARY |
| 동일 | selectSummary() | SvCustomerMapper | selectCustomerSummaryByName | SV_CUSTOMER_SUMMARY |
| 동일 | selectGrade() | SvCustomerGradeMapper | selectCustomerGrade | SV_CUSTOMER_GRADE |
| 동일 | selectProducts() | SvCustomerProductMapper | selectCustomerProducts | SV_CUSTOMER_PRODUCT |

화면 이벤트·ServiceId·DAO·Mapper SQL·DB 객체를 한 행으로 연결해야 변경 영향 분석이 가능하다.

14. Mapper Namespace 설계

14.1 고객요약 Mapper

```
com.nh.nsight.sv.customer.mapper.SvCustomerMapper
```

14.2 고객등급 Mapper

```
com.nh.nsight.sv.customer.mapper.SvCustomerGradeMapper
```

14.3 고객상품 Mapper

```
com.nh.nsight.sv.customer.mapper.SvCustomerProductMapper
```

14.4 SQL ID 기준

| SQL ID | 의미 |
| --- | --- |
| selectCustomerSummaryByNo | 고객번호 단건 조회 |
| selectCustomerSummaryByName | 고객명·지점 조회 |
| selectCustomerGrade | 기준일 최신 등급 |
| selectCustomerProducts | 보유상품 목록 |

SQL ID는 실행 행위와 대상을 명확히 표현하며, select1, getData, queryCustomer와 같은 모호한 이름을 사용하지 않는다.

15. SQL 상세 설계

15.1 고객번호 고객요약 조회

기본정보

| 항목 | 내용 |
| --- | --- |
| SQL ID | selectCustomerSummaryByNo |
| SQL 유형 | SELECT |
| 예상 결과 | 0~1건 |
| Timeout | 3초 |
| 주요 인덱스 | IX_SV_CUST_SUM_01 |
| 입력 | 고객번호·기준일 |

Mapper XML 예시

```
<select id="selectCustomerSummaryByNo"
        parameterType="com.nh.nsight.sv.customer.dto.query.SvCustomerSummaryQuery"
        resultMap="CustomerSummaryResultMap"
        timeout="3">

    SELECT
        C.CUSTOMER_NO,
        C.CUSTOMER_NAME,
        C.CUSTOMER_TYPE_CODE,
        C.MANAGEMENT_BRANCH_ID,
        C.MOBILE_NO,
        C.EMAIL_ADDRESS,
        C.MARKETING_CONSENT_YN,
        C.CUSTOMER_STATUS_CODE,
        C.DATA_AS_OF_DTM
    FROM SV_CUSTOMER_SUMMARY C
    WHERE C.CUSTOMER_NO = #{customerNo}
      AND C.BASE_DATE   = #{baseDate}

</select>
```

예상 실행계획

```
INDEX RANGE SCAN IX_SV_CUST_SUM_01
  ↓
TABLE ACCESS BY INDEX ROWID SV_CUSTOMER_SUMMARY
```

PK 컬럼 순서가 BASE_DATE, CUSTOMER_NO인 경우 고객번호 선행 조회를 위해 별도 인덱스가 필요하다.

15.2 고객명·지점 조회

```
<select id="selectCustomerSummaryByName"
        parameterType="com.nh.nsight.sv.customer.dto.query.SvCustomerSummaryQuery"
        resultMap="CustomerSummaryResultMap"
        timeout="3">

    SELECT
        C.CUSTOMER_NO,
        C.CUSTOMER_NAME,
        C.CUSTOMER_TYPE_CODE,
        C.MANAGEMENT_BRANCH_ID,
        C.MOBILE_NO,
        C.EMAIL_ADDRESS,
        C.MARKETING_CONSENT_YN,
        C.CUSTOMER_STATUS_CODE,
        C.DATA_AS_OF_DTM
    FROM SV_CUSTOMER_SUMMARY C
    WHERE C.MANAGEMENT_BRANCH_ID = #{branchId}
      AND C.CUSTOMER_NAME_NORM   = #{customerNameNormalized}
      AND C.BASE_DATE            = #{baseDate}
    ORDER BY C.CUSTOMER_NO
    FETCH FIRST 101 ROWS ONLY

</select>
```

고객명 부분검색이 필요하면 %검색어% 방식보다 Prefix 검색이나 별도 검색 인덱스 적용을 검토한다.

```
권장 가능:
CUSTOMER_NAME_NORM LIKE #{prefix} || '%'

주의:
CUSTOMER_NAME_NORM LIKE '%' || #{keyword} || '%'
```

후행 와일드카드 검색이 필수라면 일반 B-Tree 인덱스만으로 해결하기 어려우므로 Oracle Text 또는 검색전용 구조를 검토한다.

15.3 고객등급 조회

```
<select id="selectCustomerGrade"
        resultMap="CustomerGradeResultMap"
        timeout="3">

    SELECT
        G.CUSTOMER_NO,
        G.GRADE_TYPE_CODE,
        G.GRADE_CODE,
        G.GRADE_SCORE,
        G.EVALUATION_DATE,
        G.DATA_AS_OF_DTM
    FROM SV_CUSTOMER_GRADE G
    WHERE G.CUSTOMER_NO       = #{customerNo}
      AND G.GRADE_TYPE_CODE   = #{gradeTypeCode}
      AND G.EVALUATION_DATE  <= #{baseDate}
    ORDER BY G.EVALUATION_DATE DESC
    FETCH FIRST 1 ROW ONLY

</select>
```

15.4 고객상품 조회

```
<select id="selectCustomerProducts"
        resultMap="CustomerProductResultMap"
        timeout="3">

    SELECT
        P.CUSTOMER_PRODUCT_ID,
        P.PRODUCT_CODE,
        P.PRODUCT_NAME,
        P.JOIN_DATE,
        P.MATURITY_DATE,
        P.PRODUCT_STATUS_CODE,
        P.BALANCE_AMOUNT,
        P.CURRENCY_CODE,
        P.DATA_AS_OF_DTM
    FROM SV_CUSTOMER_PRODUCT P
    WHERE P.CUSTOMER_NO  = #{customerNo}
      AND P.SNAPSHOT_DATE = #{baseDate}
      AND P.PRODUCT_STATUS_CODE IN ('ACTIVE', 'SUSPENDED')
    ORDER BY
        P.JOIN_DATE DESC,
        P.CUSTOMER_PRODUCT_ID
    FETCH FIRST 101 ROWS ONLY

</select>
```

16. ResultMap 설계

16.1 고객요약 ResultMap

```
<resultMap id="CustomerSummaryResultMap"
           type="com.nh.nsight.sv.customer.dto.result.SvCustomerSummaryResult">

    <id property="customerNo"
        column="CUSTOMER_NO"/>

    <result property="customerName"
            column="CUSTOMER_NAME"/>

    <result property="customerTypeCode"
            column="CUSTOMER_TYPE_CODE"/>

    <result property="managementBranchId"
            column="MANAGEMENT_BRANCH_ID"/>

    <result property="mobileNo"
            column="MOBILE_NO"/>

    <result property="email"
            column="EMAIL_ADDRESS"/>

    <result property="marketingConsentYn"
            column="MARKETING_CONSENT_YN"/>

    <result property="customerStatusCode"
            column="CUSTOMER_STATUS_CODE"/>

    <result property="dataAsOfDtm"
            column="DATA_AS_OF_DTM"/>
</resultMap>
```

복잡한 결과는 자동 매핑보다 명시적 ResultMap을 권장한다.

17. DDL 예시

다음 DDL은 설계 예시이며 Tablespace·Storage·Partition 옵션은 DBA 표준으로 보정한다.

```
CREATE TABLE SV_CUSTOMER_SUMMARY (
    BASE_DATE                DATE            NOT NULL,
    CUSTOMER_NO              VARCHAR2(20)    NOT NULL,
    CUSTOMER_NAME            VARCHAR2(100)   NOT NULL,
    CUSTOMER_NAME_NORM       VARCHAR2(100),
    CUSTOMER_TYPE_CODE       VARCHAR2(10)    NOT NULL,
    MANAGEMENT_BRANCH_ID     VARCHAR2(10)    NOT NULL,
    MOBILE_NO                VARCHAR2(30),
    EMAIL_ADDRESS            VARCHAR2(200),
    MARKETING_CONSENT_YN     CHAR(1)         DEFAULT 'N' NOT NULL,
    CUSTOMER_STATUS_CODE     VARCHAR2(10)    NOT NULL,
    DATA_AS_OF_DTM           TIMESTAMP(6)    NOT NULL,
    CREATED_DTM              TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_DTM              TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT PK_SV_CUSTOMER_SUMMARY
        PRIMARY KEY (BASE_DATE, CUSTOMER_NO),

    CONSTRAINT CK_SV_CUST_SUM_01
        CHECK (MARKETING_CONSENT_YN IN ('Y', 'N'))
);
CREATE INDEX IX_SV_CUST_SUM_01
    ON SV_CUSTOMER_SUMMARY (
        CUSTOMER_NO,
        BASE_DATE
    );
CREATE INDEX IX_SV_CUST_SUM_02
    ON SV_CUSTOMER_SUMMARY (
        MANAGEMENT_BRANCH_ID,
        CUSTOMER_NAME_NORM,
        BASE_DATE
    );
```

18. 데이터 정합성 및 상태관리

18.1 고객요약 정합성

| 규칙 | 기준 |
| --- | --- |
| 고객 유일성 | 기준일별 고객번호 1건 |
| 고객상태 | 승인된 코드만 허용 |
| 관리지점 | 유효 지점코드 |
| 마케팅동의 | Y/N |
| 데이터기준시각 | 원천 적재 기준시각 필수 |
| 수정일시 | 데이터 변경 시 갱신 |

18.2 Snapshot 적재 원칙

```
원천 데이터 추출
  ↓
Staging 적재
  ↓
건수·Null·중복 검증
  ↓
대상 Partition 적재
  ↓
인덱스·통계 갱신
  ↓
정합성 검증
  ↓
업무 조회 전환
```

운영 조회 테이블에 부분 적재 상태가 노출되지 않도록 한다.

가능한 방식:

- Partition Exchange
- Shadow Table 후 Rename
- 적재완료 상태 플래그
- 업무 기준일 전환 테이블
18.3 중복 데이터 처리

적재 단계에서 다음을 검증한다.

```
SELECT
    BASE_DATE,
    CUSTOMER_NO,
    COUNT(*)
FROM STG_SV_CUSTOMER_SUMMARY
GROUP BY
    BASE_DATE,
    CUSTOMER_NO
HAVING COUNT(*) > 1;
```

중복이 존재하면 운영 테이블 전환을 중단한다.

19. 트랜잭션·Lock·동시성

19.1 온라인 조회

| 항목 | 기준 |
| --- | --- |
| 트랜잭션 | 읽기전용 |
| 선언 위치 | Facade |
| Isolation | DB·Spring 기본 정책 |
| Lock | 명시적 FOR UPDATE 금지 |
| Timeout | 거래·JDBC·MyBatis 3초 |
| Connection | 거래 종료 시 즉시 반환 |

19.2 배치 적재

대량 적재 중 온라인 조회와 Lock 경합이 발생하지 않도록 한다.

비권장:

```
운영 테이블 전체 DELETE
→ 대량 INSERT
→ 장시간 Commit
```

권장:

```
신규 Partition 또는 Shadow Table 적재
→ 검증
→ 짧은 전환 작업
```

19.3 DDL Lock

운영 중 다음 작업은 사전 검토 없이 수행하지 않는다.

- 컬럼 자료형 변경
- Not Null 즉시 추가
- 대형 인덱스 생성
- Table Move
- Partition Split·Merge
- FK 생성과 전체 검증
가능하면 Online DDL 옵션과 단계적 변경을 적용한다.

20. 성능·실행계획 설계

20.1 SQL별 목표

| SQL ID | 예상 건수 | 목표시간 |
| --- | --- | --- |
| selectCustomerSummaryByNo | 0~1 | 300ms |
| selectCustomerSummaryByName | 0~101 | 500ms |
| selectCustomerGrade | 0~1 | 200ms |
| selectCustomerProducts | 0~101 | 300ms |

20.2 실행계획 검증항목

| 점검 | 기준 |
| --- | --- |
| Access Path | 적절한 Index Range Scan |
| 예상 행수 | 실제와 과도한 차이 없음 |
| Join 방식 | 본 SQL은 단일 테이블 중심 |
| Sort | 불필요한 대용량 Sort 없음 |
| Predicate | Index Access와 Filter 구분 |
| Partition | 대상 Partition만 Pruning |
| Temp 사용 | 과도한 Temp 사용 없음 |
| Buffer Gets | 기준값 관리 |
| Physical Reads | 반복 조회 시 최소화 |

20.3 실행계획 증적

운영 반영 전 다음 정보를 보관한다.

```
SQL ID
DB SQL_ID
Plan Hash Value
Bind 조건 유형
테스트 데이터 건수
Elapsed Time
Buffer Gets
Rows Processed
적용 인덱스
통계정보 기준일
```

20.4 통계정보

- 대량 적재 후 통계정보를 갱신한다.
- Histogram은 데이터 편중과 Bind 민감도를 확인한 뒤 적용한다.
- 개발 DB의 소량 데이터 실행계획을 운영 기준으로 신뢰하지 않는다.
- 운영 유사 데이터 분포로 검증한다.
- 통계정보 갱신 후 주요 SQL Plan 변화를 확인한다.
20.5 SQL Hint

Hint는 최후 수단으로 사용한다.

```
통계·인덱스·SQL 구조 검토
→ Optimizer 판단 확인
→ Plan 안정성 검토
→ 필요 시 Hint 또는 SQL Plan Management
```

근거 없는 INDEX, USE_NL, PARALLEL Hint를 금지한다.

21. DB Pool·Timeout 연계

```
Tomcat Thread
  ↓
HikariCP Connection 획득
  ↓
MyBatis Mapper
  ↓
JDBC Statement
  ↓
Oracle SQL 실행
```

| Timeout | 권장 관계 |
| --- | --- |
| Hikari connectionTimeout | 약 3초 |
| TCF 거래 Timeout | 고객조회 3초 |
| Spring Transaction Timeout | 3초 |
| MyBatis Statement Timeout | 3초 이하 |
| DB Network Read Timeout | 인프라 기준 |
| 화면 Timeout | 서버보다 약간 크게 |

Timeout 계층의 값이 서로 충돌하지 않도록 한다.

SQL Timeout 정보는 Mapper ID·SQL ID·ServiceId와 연결하여 운영 추적이 가능해야 한다. 운영 화면에는 Parameter 원문 대신 Mapper와 SQL ID, 실행시간, Connection 대기시간을 표시한다.

22. 보안·개인정보·감사

22.1 개인정보 분류

| 컬럼 | 분류 | 기본 처리 |
| --- | --- | --- |
| CUSTOMER_NO | 개인 식별정보 | 접근통제·로그 마스킹 |
| CUSTOMER_NAME | 개인정보 | 응답 마스킹 |
| MOBILE_NO | 개인정보 | 암호화·응답 마스킹 |
| EMAIL_ADDRESS | 개인정보 | 암호화·응답 마스킹 |
| BALANCE_AMOUNT | 금융정보 | 기능·데이터권한 |
| MANAGEMENT_BRANCH_ID | 권한 기준정보 | 사용자 지점과 비교 |

22.2 저장 암호화

암호화 필요 여부는 농협 보안정책과 데이터 분류 기준에 따른다.

적용 후보:

- TDE Tablespace 암호화
- 민감 컬럼 암호화
- 암호화 Key 중앙관리
- 개발·검증환경 비식별화
암호화 컬럼에 검색조건이 필요하면 성능과 Key 관리 구조를 함께 검토한다.

22.3 DB 권한

| 계정 | 권한 |
| --- | --- |
| 업무 실행계정 | 필요한 Table의 최소 SELECT |
| 배치 적재계정 | 대상 Table DML |
| DDL 배포계정 | 승인된 배포 시에만 DDL |
| 조회 운영계정 | 제한적 Read Only |
| 개발자 개인계정 | 운영 직접 접근 금지 |
| 감사계정 | 감사 목적 조회만 |

업무 실행계정에 CREATE TABLE, DROP TABLE, DBA 권한을 부여하지 않는다.

22.4 SQL 로그 금지정보

다음은 일반 애플리케이션 로그에 출력하지 않는다.

```
고객번호 원문
고객명
전화번호
이메일
잔액
SQL Parameter 전체
요청·응답 Body
JWT
```

프로그램 로그도 Mapper ID와 SQL ID, 실행시간 중심으로 기록하고 개인정보를 제외해야 한다.

23. 운영·모니터링·장애 대응

23.1 운영 식별정보

| 항목 | 예시 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| Mapper | SvCustomerMapper |
| SQL ID | selectCustomerSummaryByNo |
| DB SQL_ID | Oracle 생성값 |
| Plan Hash | 실행계획 식별자 |
| 대상 객체 | SV_CUSTOMER_SUMMARY |
| 인덱스 | IX_SV_CUST_SUM_01 |

23.2 Slow SQL 수집항목

- ServiceId
- GUID·TraceId
- Mapper Namespace
- SQL ID
- Connection 획득시간
- SQL 실행시간
- 처리건수
- 오류유형
- DB SQL_ID
- Plan Hash Value
- 대상 WAR
- 발생시각
SQL Parameter와 고객정보 원문은 수집하지 않는다.

23.3 장애 확인 순서

```
거래 p95·Timeout 증가
  ↓
DB Pool Pending 확인
  ↓
Connection 획득시간과 SQL 실행시간 분리
  ↓
Mapper·SQL ID 확인
  ↓
DB SQL_ID·실행계획 확인
  ↓
Lock·Session·통계·인덱스 확인
  ↓
최근 DDL·통계·배치 변경 확인
```

23.4 주요 장애 유형

| 장애 | 원인 후보 | 조치 |
| --- | --- | --- |
| 고객번호 조회 지연 | 인덱스 미사용 | 실행계획·Bind 확인 |
| 고객명 조회 지연 | 후행 와일드카드 | 검색구조 재설계 |
| Pool Pending 증가 | Slow SQL·Lock | 장기 SQL·Session 확인 |
| Plan 급변 | 통계·Bind 민감도 | Plan 비교·통계 검토 |
| 적재 후 조회 오류 | 부분 적재 | 전환·검증 절차 확인 |
| ORA 오류 증가 | DDL·데이터 정합성 | 배포·제약조건 확인 |
| 상품 조회 과다 | 최대 건수 미적용 | Fetch 제한 적용 |

24. 정상 처리 흐름

```
1. Service가 고객번호 조회경로 선택
2. Query DTO 생성
3. DAO가 Mapper 호출
4. HikariCP Connection 획득
5. MyBatis가 Bind Variable 설정
6. Oracle이 인덱스 기반 고객요약 조회
7. 고객등급 최신 1건 조회
8. 상품 최대 101건 조회
9. DAO가 Result DTO 반환
10. Service가 초과 건수·결과 없음 판단
11. 개인정보 마스킹
12. Response DTO 조립
13. 읽기전용 트랜잭션 종료
14. Connection 반환
15. Mapper·SQL 실행시간 기록
```

25. 오류·Timeout·장애 흐름

25.1 고객 미존재

```
SQL 결과 0건
  ↓
DAO Optional.empty()
  ↓
Service가 고객 미존재 판단
  ↓
업무 오류 또는 결과 없음 응답
```

Mapper나 DAO에서 고객 미존재 오류코드를 결정하지 않는다.

25.2 SQL 오류

```
SQLException
  ↓
MyBatis·Spring DataAccessException
  ↓
DAO 기술예외 변환
  ↓
Facade Rollback
  ↓
ETF 시스템 오류
```

25.3 SQL Timeout

```
SQL 3초 초과
  ↓
JDBC Statement 취소
  ↓
Transaction Rollback
  ↓
Connection 반환 검증
  ↓
TCF-TMO-001
  ↓
Slow SQL 증적 기록
```

25.4 DB Connection 획득 실패

```
HikariCP Pending
  ↓
connectionTimeout 초과
  ↓
SQL 미실행
  ↓
DB Pool 오류
  ↓
ServiceId·Pool명·대기시간 기록
```

Connection 대기시간과 SQL 실행시간을 분리해 기록해야 원인을 정확히 판단할 수 있다.

26. 정상 예시

```
ServiceId
SV.Customer.selectSummary

DAO
SvCustomerDao.selectSummary()

Mapper
SvCustomerMapper.selectCustomerSummaryByNo

SQL
CUSTOMER_NO + BASE_DATE 조건

Index
IX_SV_CUST_SUM_01

Result
고객요약 1건

후속 SQL
고객등급 1건 + 상품 최대 100건
```

27. 금지 예시

27.1 SELECT 전체 컬럼

```
SELECT *
FROM SV_CUSTOMER_SUMMARY;
```

불필요한 I/O와 개인정보 노출 때문에 금지한다.

27.2 문자열 치환

```
WHERE CUSTOMER_NO = '${customerNo}'
```

SQL Injection과 SQL 재사용 저하 때문에 금지한다.

27.3 선택조건 OR 결합

```
WHERE (:customerNo IS NULL OR CUSTOMER_NO = :customerNo)
```

검색경로별 SQL을 분리한다.

27.4 인덱스 컬럼 함수 적용

```
WHERE TO_CHAR(BASE_DATE, 'YYYYMMDD') = :baseDate
```

다음처럼 변환된 Date 값을 Bind한다.

```
WHERE BASE_DATE = :baseDate
```

27.5 무제한 목록

```
SELECT ...
FROM SV_CUSTOMER_PRODUCT
WHERE CUSTOMER_NO = :customerNo;
```

최대 건수·페이징 기준을 적용한다.

27.6 SQL에서 권한 하드코딩

```
WHERE MANAGEMENT_BRANCH_ID = '001234'
```

검증된 권한 범위를 Parameter로 전달한다.

27.7 운영 로그 Parameter 출력

```
customerNo=CUST000001
mobileNo=01012341234
```

금지한다.

28. 책임 경계와 RACI

| 활동 | 업무팀 | DA | DBA | 아키텍처팀 | 보안팀 | 테스트팀 |
| --- | --- | --- | --- | --- | --- | --- |
| 데이터 요구사항 | A/R | C | I | C | C | C |
| 논리모델 | C | A/R | C | C | C | I |
| 물리모델 | C | C | A/R | C | C | I |
| SQL 작성 | A/R | C | C | C | I | C |
| 인덱스 설계 | C | C | A/R | C | I | C |
| 개인정보 분류 | C | C | C | C | A/R | I |
| 실행계획 검증 | R | C | A | C | I | C |
| DDL 배포 | I | C | A/R | C | C | I |
| 성능시험 | R | C | C | C | I | A |
| 장애 대응 | R | C | A/R | C | I | I |

29. 자동검증 및 품질 Gate

| 검증 ID | 검증 내용 | 실패 기준 |
| --- | --- | --- |
| DBQ-001 | Mapper Namespace 일치 | Interface와 XML 불일치 |
| DBQ-002 | SQL ID 중복 | Namespace 내 중복 |
| DBQ-003 | DAO Method–SQL ID 연결 | 미연결 |
| DBQ-004 | SELECT * 사용 | 발견 시 실패 |
| DBQ-005 | MyBatis ${} 사용 | 발견 시 실패 |
| DBQ-006 | 무제한 목록조회 | 제한조건 없음 |
| DBQ-007 | 개인정보 과다조회 | DTO 미사용 컬럼 조회 |
| DBQ-008 | PK 없는 업무 테이블 | 승인 예외 없음 |
| DBQ-009 | 인덱스 중복 | 선행컬럼 동일 중복 |
| DBQ-010 | SQL Timeout 누락 | 온라인 SQL 미설정 |
| DBQ-011 | 실행계획 증적 누락 | 주요 SQL Plan 없음 |
| DBQ-012 | 통계 갱신계획 누락 | 대량 적재 후 계획 없음 |
| DBQ-013 | ServiceId 추적 누락 | SQL 영향 거래 불명 |
| DBQ-014 | Rollback 스크립트 누락 | DDL 복구안 없음 |
| DBQ-015 | 테스트 데이터 개인정보 | 비식별화 미적용 |

30. 테스트 시나리오

30.1 SQL 기능 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| SQL-SV-001 | 정상 고객번호 | 1건 |
| SQL-SV-002 | 미존재 고객번호 | 0건 |
| SQL-SV-003 | 고객명·지점 조회 | 최대 101건 |
| SQL-SV-004 | 최신 등급 존재 | 최신 1건 |
| SQL-SV-005 | 등급 없음 | 0건 |
| SQL-SV-006 | 상품 없음 | 빈 목록 |
| SQL-SV-007 | 상품 101건 초과 | 초과 여부 확인 |

30.2 데이터 정합성 테스트

| 테스트 ID | 검증 |
| --- | --- |
| DB-SV-101 | 기준일·고객번호 중복 없음 |
| DB-SV-102 | 필수 컬럼 Null 없음 |
| DB-SV-103 | 동의여부 Y/N만 존재 |
| DB-SV-104 | 고객등급 동일 평가일 중복 없음 |
| DB-SV-105 | 상품 고객번호 참조 정합성 |
| DB-SV-106 | 데이터 기준시각 유효성 |

30.3 성능 테스트

| 테스트 ID | 시나리오 | 기준 |
| --- | --- | --- |
| PERF-SV-201 | 고객번호 단건 | 300ms 목표 |
| PERF-SV-202 | 고객명 최대조회 | 500ms 목표 |
| PERF-SV-203 | 상품 100건 | 300ms 목표 |
| PERF-SV-204 | 설계 피크 동시조회 | Pool·p95 확인 |
| PERF-SV-205 | 통계 갱신 전후 | Plan 변화 비교 |
| PERF-SV-206 | Partition 증가 | Pruning 확인 |

30.4 장애 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| FAIL-SV-301 | SQL Timeout | Statement 취소·Connection 반환 |
| FAIL-SV-302 | Pool 고갈 | SQL 미실행·표준 오류 |
| FAIL-SV-303 | DB Session Kill | 시스템 오류·자원 반환 |
| FAIL-SV-304 | 인덱스 Unusable | 탐지·배포 차단 |
| FAIL-SV-305 | 적재 중 실패 | 이전 정상 Snapshot 유지 |
| FAIL-SV-306 | DDL Rollback | 복구 절차 검증 |

31. SQL·DB 설계 체크리스트

| No. | 점검 항목 | 확인 |
| --- | --- | --- |
| 1 | ServiceId와 SQL ID가 연결되었는가 | □ |
| 2 | DAO Method와 Mapper Method가 연결되었는가 | □ |
| 3 | 대상 Table·View가 식별되었는가 | □ |
| 4 | 논리명·물리명·자료형이 정의되었는가 | □ |
| 5 | PK·Unique·Check·Not Null이 정의되었는가 | □ |
| 6 | FK의 물리 적용 여부가 결정되었는가 | □ |
| 7 | 주요 SQL의 인덱스 근거가 있는가 | □ |
| 8 | 검색경로별 SQL이 분리되었는가 | □ |
| 9 | SELECT *를 사용하지 않는가 | □ |
| 10 | ${} 문자열 치환이 없는가 | □ |
| 11 | 목록 최대 건수가 정의되었는가 | □ |
| 12 | 기준일과 Snapshot 정책이 일관적인가 | □ |
| 13 | 개인정보 최소조회 기준이 적용되었는가 | □ |
| 14 | SQL·Parameter 로그 금지 기준이 적용되었는가 | □ |
| 15 | SQL Timeout이 적용되었는가 | □ |
| 16 | 실행계획과 통계정보를 검증했는가 | □ |
| 17 | 운영 유사 데이터로 성능시험했는가 | □ |
| 18 | 배치 적재와 온라인 조회 충돌을 검토했는가 | □ |
| 19 | DDL 배포·Rollback 스크립트가 있는가 | □ |
| 20 | SQL 변경 영향 화면을 역추적할 수 있는가 | □ |

32. 변경·호환성·폐기 관리

32.1 컬럼 추가

Null 허용 컬럼 추가는 비교적 호환성이 높지만 다음을 검토한다.

- 기존 SQL 영향
- ResultMap 영향
- DTO 영향
- 적재 프로그램 영향
- 개인정보 분류
- 통계정보
- 테이블 크기와 Row Migration
필수 컬럼은 다음 순서를 권장한다.

```
Nullable 컬럼 추가
→ 기존 데이터 채움
→ 프로그램 전환
→ 검증
→ Default·Not Null 적용
```

32.2 컬럼 자료형 변경

자료형 변경은 운영 Lock과 데이터 손실 위험이 크므로 직접 변경보다 신규 컬럼·이관·전환 방식을 검토한다.

32.3 인덱스 변경

```
신규 인덱스 생성
→ 실행계획 확인
→ 부하·DML 영향 확인
→ 기존 인덱스 사용량 확인
→ 불필요 인덱스 폐기
```

기존 인덱스는 신규 인덱스 생성 직후 삭제하지 않고 관찰기간을 둔다.

32.4 SQL 변경

SQL 변경 시 다음을 함께 갱신한다.

```
Mapper XML
→ DAO
→ Result DTO
→ 실행계획
→ 성능시험
→ 추적성 매트릭스
→ 운영 Slow SQL 기준
```

32.5 테이블 폐기

```
사용 ServiceId 확인
→ SQL ID 사용처 확인
→ Mapper·DAO 참조 확인
→ 배치·보고서 참조 확인
→ 신규 접근 차단
→ Read Only 또는 폐기예정 전환
→ 보존·백업
→ 최종 Drop
```

다른 ServiceId나 배치가 사용 중이면 테이블을 삭제하지 않는다.

33. 시사점

33.1 핵심 아키텍처 판단

SQL·DB 설계의 관리 단위는 테이블 하나만이 아니다.

다음 연결이 하나의 설계 단위다.

```
ServiceId
↔ DAO Method
↔ Mapper Method
↔ SQL ID
↔ Table·Column
↔ PK·Index·Constraint
↔ 실행계획
↔ 테스트·운영로그
```

33.2 주요 위험

- 고객번호 조회와 고객명 조회를 하나의 OR SQL로 처리
- Snapshot 기준일이 서로 다른 데이터 조합
- 실제 사용 SQL과 무관한 과다 인덱스
- 고객명 부분검색으로 인한 Full Scan
- 상품 목록 무제한 반환
- 대량 적재 중 부분 데이터 노출
- SQL Timeout 후 Connection 미반환
- SQL Parameter와 개인정보 로그 출력
- DDL 변경 후 Rollback 불가
- SQL 변경이 화면 영향 분석과 연결되지 않음
33.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | 실제 논리·물리 데이터 모델 확정 |
| 2 | 검색경로별 SQL 분리 |
| 3 | 데이터 규모 기반 인덱스·파티션 확정 |
| 4 | Mapper SQL과 ServiceId 추적성 등록 |
| 5 | 개인정보 최소조회·마스킹 확정 |
| 6 | 실행계획·성능 기준선 확보 |
| 7 | 적재 전환과 Rollback 절차 수립 |
| 8 | CI/CD SQL 품질 Gate 적용 |

33.4 중장기 발전 방향

다음 정보를 자동 수집하여 SQL·DB 설계서와 실제 운영 상태의 차이를 검증해야 한다.

```
Mapper XML
+ DB Dictionary
+ Index Metadata
+ 실행계획
+ SQL 실행통계
+ ServiceId 거래로그
+ 테스트 결과
= 자동 SQL·DB 추적성 보고서
```

34. 마무리말

SV.Customer.selectSummary 거래의 SQL·DB 설계는 단순 고객 조회문 작성으로 끝나지 않는다.

```
화면 ID로 사용자 기능을 찾고,
ServiceId로 거래를 찾고,
DAO·Mapper로 SQL을 찾고,
SQL ID로 실행계획을 찾고,
DB 객체로 데이터와 인덱스를 찾고,
GUID·TraceId로 실제 장애를 찾는다.
```

모든 NSIGHT SQL과 DB 객체는 본 설계서 형식을 기준으로 프로그램설계서·거래설계서·화면설계서·테스트설계서와 연결해 관리한다.

