# mpcoa8888 CRUD 서비스 설계

## 목적과 범위

`pdmp-service`에 `TB_CR_AH_SALES_TIP_RACT` 테이블을 관리하는 독립 CRUD 프로그램 `mpcoa8888`을 추가한다. 기존 `mpcoa9999` 조회 프로그램은 변경하지 않는다. `pdmp-ui`와 저장소의 다른 프로젝트는 작업 범위에서 제외한다.

## 선택한 접근 방식

`mpcoa8888` 전용 Controller, Service, DAO, DTO, MyBatis XML을 추가한다. 기존 `mpcoa9999`를 확장하거나 공통 추상 계층을 새로 만들지 않는다. 프로그램별 계약을 분리해 기존 조회 기능에 미치는 영향을 최소화하고 현재 PDMP 네이밍 및 계층 구조를 그대로 따른다.

## 계층과 API

호출 흐름은 `mpcoa8888Controller -> mpcoa8888Service -> mpcoa8888Dao -> mpcoa8888-ORA.xml`이다. 모든 API는 `POST`이고 `StandardRequestDto`와 `StandardResponseDto` 표준 전문을 사용한다.

| 기능 | 경로 | 처리 유형 |
|---|---|---|
| 목록 | `/api/mp/co/a/8888/list` | `INQUIRY` |
| 상세 | `/api/mp/co/a/8888/detail` | `INQUIRY` |
| 등록 | `/api/mp/co/a/8888/create` | `CREATE` |
| 수정 | `/api/mp/co/a/8888/update` | `UPDATE` |
| 삭제 | `/api/mp/co/a/8888/delete` | `DELETE` |

DTO는 테이블의 일곱 컬럼과 목록 조회용 페이징 값을 표현한다. 목록은 `SALZ_TIP_KDC` 완전 일치 검색을 지원한다. 페이지 번호 기본값은 1, 페이지 크기 기본값은 20, 최대 페이지 크기는 100이다. 정렬은 `BAS_DT DESC`, `TRT_BRC`, `TRTMN_ENO` 순으로 고정한다.

## 데이터 규칙

테이블 기본키는 다음 네 컬럼의 복합키다.

- `TRT_BRC`
- `TRTMN_ENO`
- `SALZ_TIP_KDC`
- `BAS_DT`

등록은 일곱 컬럼을 저장한다. 동일한 복합키가 이미 있으면 `MP0409` 오류를 발생시킨다. 수정 시 기본키는 대상 검색에만 사용하고 `PRTO_CN`, `INQ_CN`, `INP_CN`만 변경한다. 기본키 변경은 허용하지 않는다. 삭제는 복합키로 대상을 찾아 `DELETE` SQL로 실제 행을 제거한다. 상세, 수정, 삭제 대상이 없으면 `MP0404` 오류를 발생시킨다.

## 트랜잭션

목록과 상세 Service 메서드는 읽기 전용 트랜잭션을 사용한다. 등록, 수정, 삭제 Service 메서드는 4초 제한의 쓰기 트랜잭션을 사용한다. 쓰기 처리에서 예외가 발생하거나 트랜잭션 제한 시간을 초과하면 전체 작업을 롤백한다.

구현 시 Spring의 트랜잭션 제한 시간이 실제 JDBC/MyBatis 작업에 적용되는 현재 설정을 확인한다. 단위 테스트는 Service 메서드의 선언을 검증하고, 통합 테스트는 정상 CRUD와 오류 시 데이터 일관성을 검증한다.

## TCF 거래 메타데이터

| 기능 | serviceId | transactionCode |
|---|---|---|
| 목록 | `MP.SalesTip8888.list` | `MP-INQ-8881` |
| 상세 | `MP.SalesTip8888.detail` | `MP-INQ-8882` |
| 등록 | `MP.SalesTip8888.create` | `MP-CRT-8883` |
| 수정 | `MP.SalesTip8888.update` | `MP-UPD-8884` |
| 삭제 | `MP.SalesTip8888.delete` | `MP-DEL-8885` |

Controller의 각 메서드는 위 값과 기능별 `ProcessingType`을 `@TcfTransaction`에 선언한다.

## 오류와 권한

Service는 예상 가능한 업무 오류를 `BizException`으로 발생시키고 ETF가 표준 실패 전문으로 변환한다. `exceptionCode.yml`에 다음 메시지를 추가한다.

- `MP0404`: 요청한 영업팁 실적을 찾을 수 없음
- `MP0409`: 동일한 기본키의 영업팁 실적이 이미 존재함
- 예상하지 못한 오류는 기존 `FW9999` 처리 유지

기존 인증 체계를 그대로 사용한다. 인증된 사용자는 `mpcoa8888`의 모든 API를 호출할 수 있으며 별도의 관리자 역할 제한은 추가하지 않는다.

## 테스트 전략과 완료 조건

운영 코드보다 테스트를 먼저 작성한다. 다음을 검증한다.

- 목록 검색, 고정 정렬, 기본·최대 페이징
- 복합키 상세 조회 및 없는 대상의 `MP0404`
- 정상 등록 및 중복 등록의 `MP0409`
- 내용 컬럼 수정과 기본키 불변
- 정상 물리 삭제 및 없는 대상 삭제의 `MP0404`
- 쓰기 메서드의 4초 트랜잭션 설정과 조회 메서드의 읽기 전용 설정
- Controller의 표준 전문 및 TCF 메타데이터
- DAO 메서드와 MyBatis XML SQL ID의 일치
- Oracle용 SQL과 로컬 H2 환경의 호환성
- 기존 `mpcoa9999` 동작을 포함한 `gradlew.bat test` 성공

구현은 `docs/네이밍원칙.md`와 현재 `mpcoa9999` 구조를 따른다. 테스트가 존재하지 않아 `NO-SOURCE`로 끝나는 상태는 CRUD 검증 완료로 간주하지 않는다.
