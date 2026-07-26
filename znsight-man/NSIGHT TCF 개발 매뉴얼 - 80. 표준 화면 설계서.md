NSIGHT 표준 화면 설계서

SV-CUS-0001 고객 종합정보 조회

1. 도입 전 안내말

NSIGHT 화면 설계서는 화면 배치와 입력항목만 설명하는 문서가 아니다.

사용자가 화면에 진입하고 버튼을 클릭한 시점부터 다음 항목이 어떻게 연결되는지를 정의해야 한다.

```
화면
  ↓
화면 이벤트
  ↓
입력값 검증
  ↓
ServiceId를 포함한 표준 전문
  ↓
OnlineTransactionController
  ↓
TCF → STF → Dispatcher
  ↓
Handler → Facade → Service → Rule
  ↓
DAO → Mapper → SQL → DB
  ↓
ETF 표준 응답
  ↓
화면 성공·오류 처리
```

NSIGHT TCF에서는 업무별 Controller를 생성하여 Service를 직접 호출하지 않고, 공통 온라인 Controller가 요청을 수신한 뒤 ServiceId를 기준으로 Handler를 실행한다. 따라서 화면 설계서에는 URL만 기록하는 것이 아니라 이벤트별 ServiceId, 거래코드, 권한, Timeout, 요청·응답 데이터와 화면 처리 결과를 함께 정의해야 한다.

화면 이벤트와 ServiceId는 일대일 관계로만 볼 수 없다. 하나의 화면 이벤트가 여러 ServiceId를 순차 또는 병렬 호출할 수 있고, 하나의 ServiceId가 여러 화면에서 재사용될 수 있으므로 별도의 호출 순번과 필수 여부를 관리해야 한다.

2. 문서 개요

2.1 목적

본 설계서의 목적은 SV-CUS-0001 고객 종합정보 조회 화면에 대한 다음 사항을 정의하는 것이다.

| 구분 | 목적 |
| --- | --- |
| 화면 표준화 | 화면 구성, 배치, 버튼, 입력항목 기준 정의 |
| 기능 명확화 | 사용자가 수행할 수 있는 조회·상세·팝업·다운로드 기능 정의 |
| 이벤트 추적 | UI 이벤트와 ServiceId 호출 관계 정의 |
| 프로그램 추적 | ServiceId와 Handler 이하 프로그램 연결 |
| 데이터 추적 | 요청·응답 필드와 DB 객체 관계 정의 |
| 오류 표준화 | 입력 오류, 업무 오류, 시스템 오류 처리 기준 정의 |
| 보안 통제 | 화면·기능 권한, 개인정보, 감사 기준 정의 |
| 테스트 지원 | 화면 기능과 테스트 시나리오 연결 |
| 변경관리 | 화면·이벤트·ServiceId 변경 영향 관리 |

2.2 적용범위

본 설계서는 다음 영역에 적용한다.

| 영역 | 적용 내용 |
| --- | --- |
| UI | WEBTOPSUITE 또는 NSIGHT 표준 UI |
| 화면 | 고객 종합정보 조회 |
| 업무코드 | SV |
| 업무세구분 | CUS |
| 업무 WAR | sv-service |
| Context Path | /sv |
| 온라인 Endpoint | /sv/online |
| 거래 | 고객요약, 고객상세, 접촉이력, 다운로드 |
| 보안 | 사용자·지점·기능권한·개인정보 마스킹 |
| 운영 | 거래로그·감사로그·Timeout |
| 프로그램 | Handler, Facade, Service, Rule, DAO, Mapper |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 업무 분석가 | 화면 기능과 업무 규칙 확인 |
| UI 설계자 | 화면 구성과 사용자 동선 설계 |
| UI 개발자 | 컴포넌트, 이벤트, 요청·응답 구현 |
| 업무 개발자 | ServiceId와 프로그램 구현 |
| 아키텍트 | TCF 구조와 추적성 검토 |
| 테스트 담당자 | 화면·거래 테스트 작성 |
| 보안 담당자 | 권한·개인정보·감사 검토 |
| 운영 담당자 | 오류와 거래 추적 |
| PMO·품질 담당자 | 산출물 완전성 검증 |

2.4 선행조건

- 화면 ID 체계가 확정되어 있어야 한다.
- 업무코드 SV와 업무세구분코드 CUS가 등록되어 있어야 한다.
- 고객번호와 사용자·지점 정보의 표준 형식이 정의되어 있어야 한다.
- 화면 기능권한이 OM에 등록되어 있어야 한다.
- ServiceId와 거래코드가 확정되어 있어야 한다.
- 고객 데이터의 개인정보 분류와 마스킹 기준이 승인되어 있어야 한다.
- 업무 Handler와 OM Service Catalog 등록 기준이 정의되어 있어야 한다.
2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| 화면 ID | 화면을 식별하는 고유 번호 |
| 기능 ID | 화면 내 업무 기능을 식별하는 번호 |
| 이벤트 ID | 로딩·클릭·선택·변경 등 화면 동작 식별자 |
| UI 객체 ID | 버튼, 입력필드, 그리드 등의 구현 식별자 |
| ServiceId | TCF Dispatcher가 Handler를 찾는 논리 거래 식별자 |
| 거래코드 | 거래통제·감사·통계용 식별자 |
| 화면 상태 | 초기·조회중·조회완료·오류 등 화면 실행 상태 |
| 표시 여부 | 사용자 권한이나 데이터 상태에 따른 컴포넌트 노출 기준 |
| 활성 여부 | 입력·클릭 가능 여부 |
| 마스킹 | 개인정보 일부를 숨겨 화면에 표시하는 처리 |

3. 문제 정의 및 설계 배경

3.1 문제 정의

고객 종합정보 화면은 고객 기본정보, 등급, 상품, 접촉이력 등 여러 정보를 한 화면에서 제공한다.

화면 설계가 단순한 UI 배치 중심으로 작성되면 다음 문제가 발생할 수 있다.

| 문제 | 영향 |
| --- | --- |
| 버튼과 ServiceId 연결 누락 | 어떤 거래가 실행되는지 알 수 없음 |
| 조회조건 검증 불명확 | UI와 서버 검증 결과가 달라짐 |
| 한 이벤트의 다중 호출 미표현 | 일부 영역 실패 시 처리 기준 불명확 |
| 개인정보 표시기준 누락 | 원문 데이터가 과도하게 노출될 수 있음 |
| 권한 기준 누락 | 조회·다운로드 권한이 동일하게 적용될 수 있음 |
| 오류 처리 미정의 | 화면마다 오류 표시 방식이 달라짐 |
| 화면 상태 미정의 | 중복 클릭과 중복 거래가 발생할 수 있음 |
| 프로그램 추적성 부족 | SQL 변경 시 영향 화면 식별이 어려움 |

3.2 설계 배경

본 화면은 여러 고객 데이터를 한 번에 보여주지만, 하나의 대형 ServiceId에서 모든 데이터를 일괄 처리하기보다 업무적 응집도와 장애 영향을 고려하여 다음 거래로 분리한다.

```
화면 초기화
  → SV.Customer.selectInitial

고객요약 조회
  → SV.Customer.selectSummary

고객상세 조회
  → SV.Customer.selectDetail

접촉이력 조회
  → CT.Contact.selectHistory

고객정보 다운로드
  → SV.Customer.downloadSummary
```

공통 Controller 이후에는 TCF → STF → Dispatcher → Handler → Facade → Service → Rule → DAO → Mapper의 표준 흐름을 적용한다.

4. 요구사항과 제약조건

4.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| FR-01 | 화면 진입 시 조회조건과 공통코드를 초기화해야 한다. |
| FR-02 | 고객번호 또는 대체 검색조건으로 고객을 조회할 수 있어야 한다. |
| FR-03 | 고객 기본정보와 고객등급을 표시해야 한다. |
| FR-04 | 조회 결과행 선택 시 고객 상세정보를 표시해야 한다. |
| FR-05 | 접촉이력 팝업을 호출할 수 있어야 한다. |
| FR-06 | 권한 보유자는 조회 결과를 파일로 다운로드할 수 있어야 한다. |
| FR-07 | 조회 실행 중 중복 조회를 방지해야 한다. |
| FR-08 | 고객 미존재, 권한 부족, Timeout을 구분해 표시해야 한다. |
| FR-09 | 개인정보는 사용자 권한에 따라 마스킹해야 한다. |
| FR-10 | 중요 조회와 다운로드는 감사로그를 기록해야 한다. |

4.2 비기능 요구사항

| 구분 | 기준 |
| --- | --- |
| 응답시간 | 고객요약 조회 p95 3초 이내 |
| 가용성 | 일부 부가영역 실패 시 핵심 고객요약은 표시 가능 |
| 보안 | 인증된 사용자만 접근 |
| 개인정보 | 주민번호·전화번호·주소 등 마스킹 |
| 접근성 | 키보드 이동, 명확한 레이블, 색상 외 상태표시 |
| 추적성 | ServiceId, GUID, TraceId로 거래 추적 |
| 사용성 | 조회 중 상태와 오류 원인을 사용자에게 표시 |
| 중복방지 | 조회 중 동일 이벤트 재실행 차단 |
| 호환성 | WEBTOPSUITE 표준 컴포넌트 준수 |

4.3 제약조건

- 업무별 REST Controller를 추가하지 않는다.
- 화면은 공통 /sv/online Endpoint를 호출한다.
- 고객번호 원문을 URL QueryString에 포함하지 않는다.
- 다운로드 기능은 조회 권한과 별도의 다운로드 권한을 사용한다.
- 개인정보가 포함된 응답은 브라우저 로그에 출력하지 않는다.
- UI에서 사용자 ID와 지점 ID를 임의로 변경하지 않는다.
- 인증 문맥에서 전달된 사용자 정보와 표준 Header의 정합성을 유지한다.
- 조회 결과는 서버 권한검증 결과를 기준으로 표시한다.
5. 설계 원칙

5.1 화면 ID 원칙

화면 ID는 다음 형식을 사용한다.

```
{업무코드}-{업무세구분코드}-{4자리 일련번호}
```

본 화면의 ID는 다음과 같다.

```
SV-CUS-0001
```

| 구성 | 값 | 의미 |
| --- | --- | --- |
| 업무코드 | SV | Single View |
| 업무세구분 | CUS | Customer |
| 일련번호 | 0001 | 고객 화면 순번 |

5.2 이벤트 ID 원칙

```
{화면ID}-E{2자리 순번}
```

예:

```
SV-CUS-0001-E00  화면 초기화
SV-CUS-0001-E01  고객요약 조회
SV-CUS-0001-E02  고객 상세행 선택
SV-CUS-0001-E03  접촉이력 팝업
SV-CUS-0001-E04  엑셀 다운로드
SV-CUS-0001-E05  화면 초기화 버튼
```

5.3 기능 ID 원칙

```
{화면ID}-F{2자리 순번}
```

| 기능 ID | 기능명 |
| --- | --- |
| SV-CUS-0001-F01 | 고객조회 |
| SV-CUS-0001-F02 | 고객상세조회 |
| SV-CUS-0001-F03 | 접촉이력조회 |
| SV-CUS-0001-F04 | 고객정보다운로드 |

5.4 서버 호출 원칙

```
UI 이벤트
  → 표준 전문 생성
  → ServiceId 지정
  → POST /sv/online
  → 공통 Controller
  → TCF 실행
```

화면은 Handler, Service, Mapper 클래스명을 알고 호출하지 않는다.

5.5 화면 상태 원칙

화면은 최소 다음 상태를 관리한다.

| 상태 코드 | 상태 | 설명 |
| --- | --- | --- |
| INITIAL | 초기 | 화면 최초 진입 |
| READY | 입력대기 | 검색조건 입력 가능 |
| VALIDATING | 검증중 | UI 입력값 검증 |
| LOADING | 조회중 | 서버 거래 실행 중 |
| LOADED | 조회완료 | 정상 결과 표시 |
| EMPTY | 결과없음 | 정상 처리됐으나 결과 없음 |
| ERROR | 오류 | 업무·시스템 오류 |
| DOWNLOADING | 다운로드중 | 파일 생성·전송 중 |

6. 화면 기본정보

| 항목 | 내용 |
| --- | --- |
| 화면 ID | SV-CUS-0001 |
| 화면명 | 고객 종합정보 조회 |
| 영문명 | Customer Summary View |
| 업무코드 | SV |
| 업무세구분코드 | CUS |
| 메뉴 ID | SV-MENU-CUS-001 |
| 메뉴 경로 | 고객분석 → 고객정보 → 고객 종합정보 |
| 화면 유형 | 조회형 |
| 팝업 여부 | 기본화면 |
| 상위 화면 | 없음 |
| 채널 | WEBTOP |
| 업무 WAR | sv-service |
| Context Path | /sv |
| Endpoint | POST /sv/online |
| 기본 권한 | SV_CUSTOMER_VIEW |
| 다운로드 권한 | SV_CUSTOMER_DOWNLOAD |
| 개인정보 포함 | 예 |
| 감사 대상 | 고객 상세조회·다운로드 |
| 기본 Timeout | 3초 |
| 다운로드 Timeout | 10초 |

7. 화면 레이아웃

7.1 전체 구성

```
┌──────────────────────────────────────────────────────────────────────┐
│ 고객 종합정보 조회                                    [도움말]      │
├──────────────────────────────────────────────────────────────────────┤
│ 조회조건                                                             │
│ 고객번호 [____________] 고객명 [__________] 지점 [____]              │
│ 기준일   [2026-07-15]                      [조회] [초기화] [다운로드] │
├──────────────────────────────────────────────────────────────────────┤
│ 고객 기본정보                                                        │
│ 고객번호      CUST000001      고객명       홍*동                     │
│ 고객구분      개인              관리지점     001234                    │
│ 고객등급      GOLD              마케팅동의   동의                      │
│ 휴대전화      010-****-1234    이메일       h***@example.com          │
├──────────────────────────────────────────────────────────────────────┤
│ [상품현황] [접촉이력] [캠페인이력] [상세정보]                         │
├──────────────────────────────────────────────────────────────────────┤
│ 상품현황 그리드                                                      │
│ ┌────┬───────────────┬──────────┬────────────┬────────────┐          │
│ │No. │ 상품명        │ 가입일   │ 상태       │ 잔액       │          │
│ ├────┼───────────────┼──────────┼────────────┼────────────┤          │
│ │1   │ 예금상품 A    │2025-01-03│ 정상       │ 1,000,000  │          │
│ └────┴───────────────┴──────────┴────────────┴────────────┘          │
├──────────────────────────────────────────────────────────────────────┤
│ 상태: 조회 완료 / 최종 조회시각: 2026-07-15 22:20:31                 │
└──────────────────────────────────────────────────────────────────────┘
```

7.2 영역 구성

| 영역 ID | 영역명 | 유형 | 설명 |
| --- | --- | --- | --- |
| areaHeader | 화면 제목 | Header | 화면명과 도움말 |
| areaSearch | 조회조건 | Search Panel | 고객 조회조건 입력 |
| areaCommand | 기능 버튼 | Command Bar | 조회·초기화·다운로드 |
| areaSummary | 고객 기본정보 | Summary Panel | 고객요약 표시 |
| areaTab | 상세 탭 | Tab | 상품·접촉·캠페인·상세 |
| areaGrid | 결과 그리드 | Grid | 선택 탭의 목록 표시 |
| areaStatus | 상태 영역 | Status Bar | 처리상태와 조회시각 |

8. 화면 항목 정의

8.1 조회조건 항목

| No. | 항목 ID | 항목명 | 유형 | 길이 | 필수 | 기본값 | 입력·표시 기준 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | txtCustomerNo | 고객번호 | Text | 20 | 조건 | 없음 | 영문·숫자, 공백 제거 |
| 2 | txtCustomerName | 고객명 | Text | 50 | 조건 | 없음 | 한글·영문 |
| 3 | txtBranchId | 지점코드 | Text | 6 | 조건 | 로그인 지점 | 권한 범위 내 변경 가능 |
| 4 | dtBaseDate | 기준일 | Date | 8 | 예 | 업무일 | 미래일자 불가 |
| 5 | hidUserId | 사용자 ID | Hidden | 30 | 예 | 인증 문맥 | 화면에서 변경 금지 |
| 6 | hidChannelId | 채널 ID | Hidden | 20 | 예 | WEBTOP | 고정값 |

조회조건 조합 기준

다음 중 하나의 조건은 반드시 입력해야 한다.

```
고객번호
또는
고객명 + 지점코드
```

고객번호가 입력된 경우 고객명 조건보다 우선한다.

8.2 고객 기본정보 항목

| No. | 항목 ID | 항목명 | 응답 필드 | 마스킹 | 표시 조건 |
| --- | --- | --- | --- | --- | --- |
| 1 | lblCustomerNo | 고객번호 | customerNo | 조건 | 고객조회 성공 |
| 2 | lblCustomerName | 고객명 | customerName | 예 | 고객조회 성공 |
| 3 | lblCustomerType | 고객구분 | customerTypeName | 아니오 | 항상 |
| 4 | lblBranchName | 관리지점 | managementBranchName | 아니오 | 항상 |
| 5 | lblCustomerGrade | 고객등급 | customerGradeName | 아니오 | 등급조회 성공 |
| 6 | lblMarketingConsent | 마케팅동의 | marketingConsentName | 아니오 | 권한 보유 |
| 7 | lblMobileNo | 휴대전화 | mobileNo | 예 | 개인정보 조회권한 |
| 8 | lblEmail | 이메일 | email | 예 | 개인정보 조회권한 |
| 9 | lblLastUpdateDtm | 최종변경일시 | lastUpdateDtm | 아니오 | 항상 |

8.3 상품현황 그리드

| No. | 컬럼 ID | 컬럼명 | 응답 필드 | 정렬 | 너비 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | colSeq | No. | 자동 | 중앙 | 50 | 화면 순번 |
| 2 | colProductCode | 상품코드 | productCode | 중앙 | 100 | 숨김 가능 |
| 3 | colProductName | 상품명 | productName | 좌측 | 220 | 필수 |
| 4 | colJoinDate | 가입일 | joinDate | 중앙 | 100 | 날짜 형식 |
| 5 | colStatusName | 상태 | statusName | 중앙 | 90 | 코드명 |
| 6 | colBalanceAmt | 잔액 | balanceAmount | 우측 | 140 | 금액 형식 |
| 7 | colMaturityDate | 만기일 | maturityDate | 중앙 | 100 | 해당 시 표시 |

9. 버튼 및 기능 정의

| 버튼 ID | 버튼명 | 기능 ID | 권한 | 활성 조건 | 처리 |
| --- | --- | --- | --- | --- | --- |
| btnSearch | 조회 | F01 | SV_CUSTOMER_VIEW | 조회조건 유효 | 고객요약 조회 |
| btnReset | 초기화 | 없음 | 화면접근권한 | 항상 | 조건·결과 초기화 |
| btnContact | 접촉이력 | F03 | CT_CONTACT_VIEW | 고객 선택 완료 | 접촉이력 팝업 |
| btnDownload | 다운로드 | F04 | SV_CUSTOMER_DOWNLOAD | 결과 존재 | 파일 생성·다운로드 |
| btnHelp | 도움말 | 없음 | 화면접근권한 | 항상 | 도움말 팝업 |

9.1 버튼 공통 처리

- 서버 호출 중 관련 버튼을 비활성화한다.
- 버튼을 연속 클릭해도 동일 거래가 중복 실행되지 않아야 한다.
- 권한이 없는 버튼은 숨김을 원칙으로 하되, 화면 정책상 표시가 필요한 경우 비활성화한다.
- 조회 결과가 없으면 다운로드 버튼을 비활성화한다.
- 다운로드 실행 전 개인정보 포함 사실을 안내한다.
10. 화면 이벤트 정의

| 이벤트 ID | 이벤트명 | 이벤트 유형 | UI 객체 | 선행조건 | 서버 호출 | 성공 처리 | 실패 처리 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E00 | 화면 초기화 | Load | screen.onload | 화면접근권한 | SV.Customer.selectInitial | 조건 초기화 | 화면 진입 오류 |
| E01 | 고객요약 조회 | Click | btnSearch | 조회조건 유효 | SV.Customer.selectSummary | 요약·상품 표시 | 조건 유지 후 오류 |
| E02 | 고객 상세행 선택 | Row Select | grdCustomer | 조회행 선택 | SV.Customer.selectDetail | 상세 탭 표시 | 선택행 유지 |
| E03 | 접촉이력 팝업 | Click | btnContact | 고객 선택 | CT.Contact.selectHistory | 팝업 표시 | 팝업 미오픈 |
| E04 | 고객정보 다운로드 | Click | btnDownload | 결과·권한 존재 | SV.Customer.downloadSummary | 파일 전달 | 다운로드 오류 |
| E05 | 화면 초기화 | Click | btnReset | 없음 | 없음 | 조건·결과 삭제 | 해당 없음 |
| E06 | 고객번호 변경 | Change | txtCustomerNo | 값 변경 | 없음 | 종속 결과 초기화 | 해당 없음 |

11. 이벤트–ServiceId–거래 매핑

| 이벤트 ID | 호출 순번 | ServiceId | 거래코드 | 처리유형 | 필수 여부 | Timeout | 감사 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E00 | 1 | SV.Customer.selectInitial | SV-INQ-0000 | 조회 | 필수 | 3초 | N |
| E01 | 1 | SV.Customer.selectSummary | SV-INQ-0001 | 조회 | 필수 | 3초 | Y |
| E02 | 1 | SV.Customer.selectDetail | SV-INQ-0002 | 조회 | 필수 | 3초 | Y |
| E03 | 1 | CT.Contact.selectHistory | CT-INQ-0001 | 조회 | 필수 | 3초 | Y |
| E04 | 1 | SV.Customer.downloadSummary | SV-DWN-0001 | 다운로드 | 필수 | 10초 | Y |

12. ServiceId–프로그램 매핑

| ServiceId | Handler.Method | Facade.Method | Service.Method | Rule.Method |
| --- | --- | --- | --- | --- |
| SV.Customer.selectInitial | SvCustomerHandler.handleSelectInitial | SvCustomerFacade.selectInitial | SvCustomerService.selectInitial | SvCustomerInquiryRule.validateInitial |
| SV.Customer.selectSummary | SvCustomerHandler.handleSelectSummary | SvCustomerFacade.selectSummary | SvCustomerService.selectSummary | SvCustomerInquiryRule.validateInquiry |
| SV.Customer.selectDetail | SvCustomerHandler.handleSelectDetail | SvCustomerFacade.selectDetail | SvCustomerService.selectDetail | SvCustomerInquiryRule.validateDetailInquiry |
| CT.Contact.selectHistory | CtContactHandler.handleSelectHistory | CtContactFacade.selectHistory | CtContactService.selectHistory | CtContactInquiryRule.validate |
| SV.Customer.downloadSummary | SvCustomerHandler.handleDownloadSummary | SvCustomerFacade.downloadSummary | SvCustomerDownloadService.createDownloadFile | SvCustomerDownloadRule.validate |

12.1 데이터 접근 매핑

| ServiceId | DAO.Method | Mapper Method | SQL ID | DB 객체 |
| --- | --- | --- | --- | --- |
| SV.Customer.selectInitial | SvCustomerDao.selectInitialCodes | SvCustomerMapper.selectInitialCodes | selectInitialCodes | OM_COMMON_CODE |
| SV.Customer.selectSummary | SvCustomerDao.selectSummary | SvCustomerMapper.selectSummary | selectCustomerSummary | SV_CUSTOMER_SUMMARY |
| SV.Customer.selectSummary | SvCustomerDao.selectGrade | SvCustomerMapper.selectGrade | selectCustomerGrade | SV_CUSTOMER_GRADE |
| SV.Customer.selectDetail | SvCustomerDao.selectDetail | SvCustomerMapper.selectDetail | selectCustomerDetail | SV_CUSTOMER_DETAIL |
| CT.Contact.selectHistory | CtContactDao.selectHistory | CtContactMapper.selectHistory | selectContactHistory | CT_CONTACT_HISTORY |
| SV.Customer.downloadSummary | SvCustomerDao.selectDownloadData | SvCustomerMapper.selectDownloadData | selectCustomerDownloadData | SV_CUSTOMER_SUMMARY |

13. 표준 전문 설계

13.1 고객요약 조회 요청

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "",
    "branchId": "",
    "guid": "",
    "traceId": ""
  },
  "body": {
    "customerNo": "CUST000001",
    "customerName": "",
    "searchBranchId": "001234",
    "baseDate": "20260715"
  }
}
```

13.2 요청 필드 매핑

| 화면 항목 | 요청 필드 | 필수 | 변환 기준 |
| --- | --- | --- | --- |
| txtCustomerNo | customerNo | 조건 | 공백 제거·대문자 변환 |
| txtCustomerName | customerName | 조건 | 앞뒤 공백 제거 |
| txtBranchId | searchBranchId | 조건 | 6자리 |
| dtBaseDate | baseDate | 예 | yyyyMMdd |
| 인증 문맥 | header.userId | 예 | 화면 입력값 사용 금지 |
| 인증 문맥 | header.branchId | 예 | 로그인 지점 |

13.3 고객요약 조회 응답

```
{
  "header": {
    "serviceId": "SV.Customer.selectSummary",
    "guid": "G202607150000001",
    "traceId": "T202607150000001"
  },
  "result": {
    "code": "S0000",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "customer": {
      "customerNo": "CUST000001",
      "customerName": "홍*동",
      "customerTypeName": "개인",
      "managementBranchName": "서울중앙지점",
      "mobileNo": "010-****-1234",
      "email": "h***@example.com"
    },
    "grade": {
      "customerGradeCode": "GOLD",
      "customerGradeName": "GOLD"
    },
    "products": []
  }
}
```

13.4 응답 필드 처리

| 응답 필드 | 화면 영역 | 결과 없음 처리 |
| --- | --- | --- |
| customer | 고객 기본정보 | 전체 영역 초기화 |
| grade | 고객등급 | 등급 없음 표시 |
| products | 상품현황 그리드 | 빈 그리드 표시 |
| result.code | 오류 처리 | 코드별 메시지 처리 |
| guid | 장애 추적 | 사용자 화면에는 필요 시 축약 표시 |

14. 정상 처리 흐름

14.1 고객요약 조회

```
1. 사용자가 고객번호 입력
   ↓
2. 조회 버튼 클릭
   ↓
3. UI 필수값·형식 검증
   ↓
4. 화면 상태를 LOADING으로 변경
   ↓
5. 조회·다운로드 버튼 비활성화
   ↓
6. SV.Customer.selectSummary 요청
   ↓
7. TCF/STF 인증·권한·거래통제·Timeout 검증
   ↓
8. Handler → Facade → Service → Rule
   ↓
9. DAO → Mapper → DB 조회
   ↓
10. ETF 표준 응답
   ↓
11. 고객요약과 상품현황 표시
   ↓
12. 화면 상태를 LOADED로 변경
   ↓
13. 최종 조회시각 표시
```

14.2 결과 없음

```
정상 응답 + 결과 0건
  ↓
화면 상태 EMPTY
  ↓
기존 조회결과 삭제
  ↓
“조회된 고객정보가 없습니다.” 표시
  ↓
조회조건 유지
```

15. 오류·Timeout·장애 흐름

15.1 오류 분류

| 오류 유형 | 예시 | 화면 처리 |
| --- | --- | --- |
| 입력 오류 | 고객번호 형식 오류 | 해당 필드 포커스 |
| 업무 오류 | 고객 미존재 | 안내 메시지 |
| 권한 오류 | 타 지점 고객 조회 불가 | 결과 미표시 |
| 인증 오류 | Token 만료 | 재인증 절차 |
| Timeout | 3초 초과 | 재시도 안내 |
| 시스템 오류 | DB·네트워크 오류 | 공통 오류 메시지 |
| 부분 오류 | 고객등급 조회 실패 | 기본정보 표시, 등급 오류 표시 |

15.2 표준 오류코드

| 오류코드 | 오류명 | 사용자 메시지 | 화면 상태 |
| --- | --- | --- | --- |
| SV-CUS-001 | 조회조건 누락 | 고객번호 또는 고객명과 지점을 입력하십시오. | READY |
| SV-CUS-002 | 고객 미존재 | 조회된 고객정보가 없습니다. | EMPTY |
| SV-CUS-003 | 데이터 권한 없음 | 해당 고객정보를 조회할 권한이 없습니다. | ERROR |
| TCF-AUTH-001 | 인증 만료 | 로그인 정보가 만료되었습니다. | ERROR |
| TCF-TMO-001 | 거래 Timeout | 처리시간이 초과되었습니다. 잠시 후 다시 시도하십시오. | ERROR |
| TCF-SYS-001 | 시스템 오류 | 처리 중 오류가 발생했습니다. | ERROR |

15.3 Timeout 처리

- Timeout 발생 시 자동 재호출하지 않는다.
- 조회조건은 유지한다.
- 기존 결과는 새 거래가 실패한 경우 삭제하지 않고, 데이터가 이전 조회 결과임을 표시한다.
- 오류 메시지와 함께 추적번호를 제공할 수 있다.
- 등록·변경 거래와 달리 조회 거래는 사용자가 명시적으로 재시도할 수 있다.
16. 상태 및 데이터 관리

16.1 화면 상태 모델

```
INITIAL
  ↓
READY
  ↓ 조회
VALIDATING
  ├─ 실패 → READY
  └─ 성공 → LOADING
                ├─ 결과 있음 → LOADED
                ├─ 결과 없음 → EMPTY
                └─ 오류 → ERROR
```

16.2 데이터 초기화 기준

| 동작 | 조회조건 | 고객요약 | 그리드 | 선택행 |
| --- | --- | --- | --- | --- |
| 화면 진입 | 초기화 | 초기화 | 초기화 | 해제 |
| 조회 성공 | 유지 | 신규 결과 | 신규 결과 | 해제 |
| 결과 없음 | 유지 | 초기화 | 빈 목록 | 해제 |
| 조회 오류 | 유지 | 원칙적 유지 | 원칙적 유지 | 유지 |
| 초기화 버튼 | 초기화 | 초기화 | 초기화 | 해제 |
| 고객번호 변경 | 변경 | 초기화 | 초기화 | 해제 |

16.3 브라우저 저장 금지

다음 정보는 localStorage나 일반 브라우저 저장소에 장기 저장하지 않는다.

- 고객번호
- 고객명
- 전화번호
- 이메일
- 상품잔액
- 전체 응답 전문
- 접근 Token의 로그 출력값
17. 보안·개인정보·감사

17.1 권한 기준

| 권한코드 | 기능 |
| --- | --- |
| SV_CUSTOMER_VIEW | 고객 기본정보 조회 |
| SV_CUSTOMER_DETAIL | 고객 상세정보 조회 |
| CT_CONTACT_VIEW | 접촉이력 조회 |
| SV_CUSTOMER_DOWNLOAD | 고객정보 다운로드 |
| SV_CUSTOMER_UNMASK | 개인정보 원문 조회 |

17.2 마스킹 기준

| 항목 | 일반 사용자 | 원문조회 권한자 |
| --- | --- | --- |
| 고객명 | 홍*동 | 홍길동 |
| 휴대전화 | 010-****-1234 | 010-1234-1234 |
| 이메일 | h***@example.com | hong@example.com |
| 주민등록번호 | 화면 표시 금지 | 별도 승인 화면만 허용 |
| 주소 | 시·군·구 이하 마스킹 | 업무 승인 시 표시 |

17.3 감사로그

다음 행위는 감사로그 대상이다.

- 고객 상세정보 조회
- 개인정보 원문 조회
- 접촉이력 조회
- 고객정보 다운로드
- 권한 오류가 발생한 조회 시도
감사로그에는 다음 정보를 기록한다.

```
GUID
TraceId
ServiceId
거래코드
사용자 ID
지점 ID
화면 ID
이벤트 ID
대상 고객 식별자
실행시각
결과
다운로드 건수
실패 사유
```

18. 성능·용량·확장성

| 항목 | 기준 |
| --- | --- |
| 기본 조회 | p95 3초 이내 |
| 상세 조회 | p95 3초 이내 |
| 다운로드 | 최대 10초 또는 비동기 전환 |
| 조회 최대 건수 | 화면 그리드 500건 |
| 페이지 크기 | 기본 50건 |
| 중복 클릭 | 거래 실행 중 차단 |
| 대량 데이터 | 화면 직접 렌더링 금지 |
| 다운로드 건수 | 기준 초과 시 비동기 파일 생성 |
| 캐시 | 공통코드·상품코드 등 기준정보만 적용 |
| 개인정보 | 클라이언트 캐시 금지 |

19. 책임 경계와 RACI

| 활동 | 업무팀 | UI팀 | 프레임워크팀 | 아키텍처팀 | 보안팀 | 테스트팀 |
| --- | --- | --- | --- | --- | --- | --- |
| 화면 기능 정의 | A/R | C | I | C | I | C |
| 화면 레이아웃 | C | A/R | I | C | C | C |
| 이벤트 정의 | C | R | C | A | I | C |
| ServiceId 정의 | R | C | C | A | I | C |
| 표준 전문 구현 | C | R | C | A | I | C |
| Handler 이하 구현 | R | I | C | A | I | C |
| 개인정보 기준 | C | C | I | C | A/R | C |
| 테스트 작성 | C | C | C | C | C | A/R |
| 운영 기준정보 | R | I | C | A | C | C |

20. 정상 예시

```
고객번호 CUST000001 입력
→ 조회 버튼 클릭
→ UI 입력검증 성공
→ SV.Customer.selectSummary 호출
→ 권한·거래통제·Timeout 검증 성공
→ 고객요약과 등급 조회
→ 개인정보 마스킹 적용
→ 화면 결과 표시
→ 감사로그 기록
```

정상 처리 시 조회조건은 유지하고, 결과 영역만 새 데이터로 교체한다.

21. 금지 예시

21.1 업무 API 직접 호출

```
화면
→ /sv/customer/search
→ SvCustomerController
→ Service
```

금지한다.

모든 온라인 거래는 표준 전문과 ServiceId를 사용하여 공통 온라인 Endpoint로 호출한다.

21.2 사용자 정보 임의 설정

```
request.header.userId = document.querySelector("#userId").value;
```

금지한다.

사용자와 지점 정보는 검증된 인증 문맥을 기준으로 한다.

21.3 개인정보 로그 출력

```
console.log(response.body.customer);
```

금지한다.

21.4 Token·고객번호 URL 전달

```
/sv/customer?token=...&customerNo=CUST000001
```

금지한다.

21.5 오류 발생 후 결과를 정상값처럼 유지

이전 조회 결과를 유지하는 경우 반드시 다음과 같이 표시한다.

```
현재 조회는 실패했습니다.
화면에는 2026-07-15 22:10:31에 조회한 이전 결과가 표시되어 있습니다.
```

22. 자동검증 및 품질 Gate

| 검증 ID | 검증 내용 | 실패 기준 |
| --- | --- | --- |
| SCR-001 | 화면 ID 형식 | 표준 패턴 불일치 |
| SCR-002 | 이벤트 ID 중복 | 동일 화면 내 중복 |
| SCR-003 | 서버 호출 이벤트 ServiceId | ServiceId 누락 |
| SCR-004 | ServiceId Handler 등록 | Handler 미등록 |
| SCR-005 | 거래코드 등록 | 거래코드 미등록 |
| SCR-006 | 권한코드 등록 | OM 권한 미등록 |
| SCR-007 | Timeout 정책 | ServiceId 정책 누락 |
| SCR-008 | 감사 대상 정의 | 중요거래 미정의 |
| SCR-009 | 개인정보 마스킹 | 마스킹 기준 누락 |
| SCR-010 | UI 필드와 DTO 매핑 | 요청·응답 미매핑 |
| SCR-011 | 테스트 케이스 연결 | 기능별 테스트 누락 |
| SCR-012 | 폐기 ServiceId 참조 | 폐기 거래 호출 |

운영 반영 전 다음 기준을 충족해야 한다.

```
화면 이벤트 정의율 100%
서버 호출 이벤트–ServiceId 연결률 100%
ServiceId–Handler 연결률 100%
개인정보 항목 마스킹 정의율 100%
권한·감사·Timeout 정의율 100%
필수 테스트 성공률 100%
Blocker 0건
Critical 0건
```

23. 테스트 시나리오

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TC-SV-0001-001 | 화면 최초 진입 | 공통코드·기준일 초기화 |
| TC-SV-0001-010 | 정상 고객번호 조회 | 고객요약 정상 표시 |
| TC-SV-0001-011 | 고객번호 공란 조회 | 입력 오류와 포커스 이동 |
| TC-SV-0001-012 | 존재하지 않는 고객 | 결과 없음 표시 |
| TC-SV-0001-013 | 타 지점 고객 조회 | 권한 오류 |
| TC-SV-0001-014 | 조회 버튼 연속 클릭 | 거래 1회만 실행 |
| TC-SV-0001-020 | 고객 상세행 선택 | 상세정보 표시 |
| TC-SV-0001-030 | 접촉이력 팝업 | 팝업 정상 표시 |
| TC-SV-0001-031 | 접촉이력 권한 없음 | 버튼 숨김 또는 비활성 |
| TC-SV-0001-040 | 다운로드 정상 | 파일 생성·감사로그 기록 |
| TC-SV-0001-041 | 다운로드 권한 없음 | 실행 차단 |
| TC-SV-0001-050 | 거래 Timeout | 오류 표시·조건 유지 |
| TC-SV-0001-051 | 인증 Token 만료 | 재인증 처리 |
| TC-SV-0001-060 | 개인정보 일반권한 | 마스킹 표시 |
| TC-SV-0001-061 | 개인정보 원문권한 | 승인 범위 내 원문 표시 |

24. 체크리스트

| No. | 점검 항목 | 확인 |
| --- | --- | --- |
| 1 | 화면 ID와 메뉴 ID가 정의되었는가 | □ |
| 2 | 화면 접근권한이 등록되었는가 | □ |
| 3 | 모든 UI 객체에 고유 ID가 있는가 | □ |
| 4 | 입력항목의 길이·필수·형식이 정의되었는가 | □ |
| 5 | 버튼 활성·비활성 조건이 정의되었는가 | □ |
| 6 | 모든 서버 호출 이벤트에 ServiceId가 있는가 | □ |
| 7 | 거래코드와 처리유형이 정의되었는가 | □ |
| 8 | 요청·응답 DTO 매핑이 정의되었는가 | □ |
| 9 | 정상·결과 없음·오류 흐름이 구분되었는가 | □ |
| 10 | Timeout 처리기준이 정의되었는가 | □ |
| 11 | 개인정보 마스킹 기준이 있는가 | □ |
| 12 | 다운로드 권한이 조회권한과 분리되었는가 | □ |
| 13 | 감사로그 대상이 정의되었는가 | □ |
| 14 | 중복 클릭 방지기준이 있는가 | □ |
| 15 | 화면 상태 전이 기준이 정의되었는가 | □ |
| 16 | ServiceId와 Handler가 연결되었는가 | □ |
| 17 | DAO·Mapper·DB 객체가 추적되는가 | □ |
| 18 | 이벤트별 테스트 케이스가 있는가 | □ |
| 19 | 접근성과 키보드 동작이 검증되었는가 | □ |
| 20 | 변경 시 영향 산출물이 정의되었는가 | □ |

25. 변경·호환성·폐기 관리

25.1 화면 항목 변경

화면 표시항목 추가 시 다음 항목을 검토한다.

```
화면 항목
→ 응답 DTO
→ Service
→ DAO·Mapper
→ DB 객체
→ 개인정보 분류
→ 권한
→ 테스트
```

25.2 이벤트 변경

이벤트 ID는 외부 추적 기준으로 사용하므로 단순 버튼명 변경만으로 삭제하지 않는다.

```
btnSearch → btnCustomerSearch
이벤트 ID E01 유지
```

업무 의미가 변경될 때만 신규 이벤트 ID를 부여한다.

25.3 ServiceId 변경

ServiceId 변경 시 다음 항목을 동시에 변경해야 한다.

- UI 호출 정의
- Handler Registry
- OM Service Catalog
- 거래통제
- Timeout 정책
- 권한정보
- 감사정책
- 테스트 케이스
- 거래로그 검색기준
- 추적성 매트릭스
25.4 화면 폐기

```
메뉴 노출 중지
→ 신규 접근 차단
→ 화면 이벤트 사용 여부 확인
→ ServiceId 타 화면 재사용 여부 확인
→ 미사용 Handler·프로그램 분석
→ 운영로그 보존
→ 화면·ServiceId 최종 폐기
```

화면이 폐기되어도 다른 화면이 동일 ServiceId를 사용하고 있으면 해당 ServiceId와 프로그램을 제거해서는 안 된다.

26. 시사점

26.1 핵심 아키텍처 판단

화면 설계서는 다음 관계를 완결해야 한다.

```
화면 구성
↔ 화면 이벤트
↔ ServiceId
↔ 표준 전문
↔ 프로그램
↔ 데이터
↔ 권한
↔ 감사
↔ 테스트
```

화면 버튼과 URL만 정의한 문서는 NSIGHT TCF 기준의 완전한 화면 설계서가 아니다.

26.2 주요 위험

- 이벤트는 있으나 ServiceId가 없는 경우
- 조회권한과 다운로드권한을 동일하게 적용하는 경우
- 고객정보를 화면이나 브라우저 로그에 원문으로 출력하는 경우
- Timeout 후 이전 결과를 최신 결과처럼 표시하는 경우
- 화면 변경 후 DTO·Mapper·테스트가 갱신되지 않는 경우
- 화면 폐기 시 재사용 ServiceId까지 제거하는 경우
26.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | 전체 화면의 화면 ID·이벤트 ID 확정 |
| 2 | 이벤트별 ServiceId와 거래코드 매핑 |
| 3 | 화면 필드와 요청·응답 DTO 매핑 |
| 4 | 기능권한·개인정보·감사 기준 연결 |
| 5 | ServiceId 이하 프로그램 추적성 연결 |
| 6 | 테스트 케이스와 품질 Gate 적용 |

26.4 중장기 발전 방향

화면 설계정보는 향후 다음 항목에서 자동 추출·검증하는 구조로 발전해야 한다.

```
화면 메타정보
+ 이벤트 정의
+ ServiceId 호출 소스
+ Handler Registry
+ OM Service Catalog
+ Mapper XML
+ 테스트 결과
= 자동 화면 추적성 보고서
```

27. 마무리말

본 화면 설계서는 화면의 외형만 정의하는 문서가 아니다.

사용자의 화면 동작에서 시작하여 TCF 거래, 프로그램, 데이터, 보안, 운영, 테스트까지 연결하는 실행 가능한 설계 기준이다.

따라서 SV-CUS-0001 고객 종합정보 조회 화면은 다음 기준으로 관리한다.

```
화면 ID를 기준으로 화면을 관리하고,
이벤트 ID를 기준으로 사용자 동작을 관리하며,
ServiceId를 기준으로 거래를 관리하고,
GUID·TraceId를 기준으로 운영 장애를 추적한다.
```

이 구조를 NSIGHT의 조회·등록·변경·삭제·팝업·다운로드 화면에 공통 적용한다.

