# 화면번호와 ServiceId 연결

> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

화면번호와 ServiceId 연결 설계기준
## 1. 도입 전 안내말

화면번호와 ServiceId 연결 기준은 “이 화면에서 어떤 거래를 호출할 수 있는가”를 정의하는 기준이다.
NSIGHT TCF에서는 URL이 아니라 serviceId가 실제 업무 실행 기준이다. 따라서 화면번호는 단순 UI 식별자가 아니라 다음 항목과 연결되어야 한다.
```text
MenuId
↓
ScreenId
↓
FunctionCode
↓
ServiceId
↓
TransactionCode
↓
```

권한검증 / 거래통제 / 거래로그 / 감사로그

핵심 원칙은 다음이다.
화면은 ServiceId를 직접 마음대로 호출하지 않는다.OM에 등록된 화면번호와 ServiceId 매핑 기준을 통과한 거래만 실행한다.

## 2. 설계 결론

화면번호와 ServiceId는 직접 1:1 고정 관계가 아니라 “화면 + 기능 + ServiceId” 관계로 연결한다.
OM_SCREEN
= 화면 마스터

OM_SERVICE_CATALOG
= ServiceId 마스터

OM_SCREEN_SERVICE_MAP
= 화면에서 호출 가능한 ServiceId 매핑

OM_SCREEN_FUNCTION_AUTH
= 화면 기능권한

| 구분 | 설계 기준 |
| --- | --- |
| 화면 식별 | SCREEN_ID |
| 서비스 식별 | SERVICE_ID |
| 기능 식별 | FUNCTION_CODE |
| 거래 식별 | TRANSACTION_CODE |
| 연결 기준 | SCREEN_ID + FUNCTION_CODE + SERVICE_ID |
| 검증 위치 | TCF STF.preProcess() |
| 관리 주체 | OM 운영관리 |
| 기본 정책 | 등록되지 않은 화면-ServiceId 호출은 차단 |
| 로그 기준 | 거래로그에 screenId, functionCode, serviceId를 함께 기록 |
| 감사 기준 | 다운로드, 고객상세조회, 권한변경 등은 감사로그 대상 |

## 3. 왜 1:1이 아니라 1:N 구조인가

하나의 화면은 보통 여러 ServiceId를 호출한다.
예를 들어 SVLIST0001, 고객요약조회 화면은 다음 서비스를 사용할 수 있다.
| 화면 | 기능 | ServiceId |
| --- | --- | --- |
| 고객요약조회 화면 | 초기조회 | SV.Customer.selectSummary |
| 고객요약조회 화면 | 목록조회 | SV.Customer.selectList |
| 고객요약조회 화면 | 상세조회 | SV.Customer.selectDetail |
| 고객요약조회 화면 | 엑셀다운로드 | SV.Customer.downloadExcel |
| 고객요약조회 화면 | 공통코드조회 | CC.CommonCode.selectList |

따라서 화면과 ServiceId는 다음 구조가 맞다.
1개 ScreenId
```text
   ├─ SEARCH 기능 → 조회 ServiceId
   ├─ DETAIL 기능 → 상세 ServiceId
   ├─ SAVE 기능   → 저장 ServiceId
   ├─ DELETE 기능 → 삭제 ServiceId
   └─ DOWNLOAD 기능 → 다운로드 ServiceId
```

## 4. 연결 구조도

```text
[사용자 화면]
  screenId = SVLIST0001
  functionCode = SEARCH
        ↓
[표준 전문 Header]
  menuId
  screenId
  functionCode
  serviceId
  transactionCode
  businessCode
        ↓

```

[STF 전처리]
## 1. Header 필수값 검증

## 2. 화면번호 존재 여부 확인

## 3. 화면-ServiceId 매핑 확인

## 4. 기능권한 확인

## 5. 거래통제 확인

## 6. Timeout 정책 확인

```text
        ↓
[TransactionDispatcher]
  serviceId 기준 Handler 실행
        ↓

```

```text
[Handler → Facade → Service → Rule → DAO]
        ↓
[ETF 후처리]
  거래로그 / 감사로그 / 표준응답 조립

```

## 5. 표준 Header 기준

화면에서 ServiceId를 호출할 때 Header에는 최소 다음 항목이 있어야 한다.
| Header 항목 | 설명 |
| --- | --- |
| 예시 | businessCode |
| 업무코드 | SV |
| menuId | 메뉴 ID |
| SVMENU0002 | screenId |
| 화면번호 | SVLIST0001 |
| functionCode | 화면 기능코드 |
| SEARCH | serviceId |
| 실행 ServiceId | SV.Customer.selectSummary |
| transactionCode | 거래코드 |
| SV-INQ-0001 | userId |
| 사용자 ID | U123456 |
| branchId | 지점코드 |
| 001234 | channelId |
| 채널 ID | WEBTOP |
| guid | 거래 GUID |
| 자동 생성 또는 전달 | traceId |
| Trace ID | 자동 생성 |

## 6. FunctionCode 표준

화면의 버튼 또는 기능 단위는 FUNCTION_CODE로 관리한다.
| FunctionCode | 의미 | 대표 ServiceId 예시 |
| --- | --- | --- |
| INIT | 화면 초기화 | SV.Customer.init |
| SEARCH | 목록/요약 조회 | SV.Customer.selectSummary |
| DETAIL | 상세 조회 | SV.Customer.selectDetail |
| CREATE | 신규 등록 | CM.Campaign.create |
| UPDATE | 수정 | CM.Campaign.update |
| DELETE | 삭제 | OM.User.delete |
| SAVE | 저장 | CM.Campaign.save |
| UPLOAD | 업로드 | CM.Target.upload |
| DOWNLOAD | 다운로드 | SV.Customer.downloadExcel |
| APPROVE | 승인 | OM.Deploy.approve |
| REJECT | 반려 | OM.Deploy.reject |
| POPUP | 팝업 조회 | SV.Customer.searchPopup |
| COMMON | 공통 조회 | CC.CommonCode.selectList |

## 7. 테이블 설계 기준

### 7.1 OM_SCREEN 화면 마스터

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| SCREEN_ID | 화면번호 | SVLIST0001 |
| SCREEN_NAME | 화면명 | 고객요약조회 |
| BUSINESS_CODE | 업무코드 | SV |
| SCREEN_TYPE | 화면유형 | LIST |
| SCREEN_PATH | 화면 경로 | /ui/sv/customer/sv-customer-list.html |
| MENU_ID | 기본 메뉴 ID | SVMENU0002 |
| OWNER_TEAM | 담당팀 | SV개발팀 |
| AUDIT_YN | 감사 대상 여부 | Y |
| USE_YN | 사용 여부 | Y |
| DESCRIPTION | 설명 | 고객 요약 조회 화면 |

### 7.2 OM_SERVICE_CATALOG ServiceId 마스터

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| SERVICE_ID | ServiceId | SV.Customer.selectSummary |
| SERVICE_NAME | 서비스명 | 고객요약조회 |
| BUSINESS_CODE | 업무코드 | SV |
| TRANSACTION_CODE | 거래코드 | SV-INQ-0001 |
| HANDLER_BEAN_NAME | Handler Bean | svCustomerSummaryHandler |
| SERVICE_TYPE | 서비스 유형 | INQUIRY |
| TIMEOUT_SECONDS | 기본 Timeout | 3 |
| AUTH_REQUIRED_YN | 권한 필요 여부 | Y |
| AUDIT_YN | 감사로그 여부 | Y |
| USE_YN | 사용 여부 | Y |

### 7.3 OM_SCREEN_SERVICE_MAP 화면-ServiceId 매핑

이 테이블이 핵심이다.
| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| MAP_ID | 매핑 ID | SSM202607050001 |
| SCREEN_ID | 화면번호 | SVLIST0001 |
| FUNCTION_CODE | 기능코드 | SEARCH |
| SERVICE_ID | ServiceId | SV.Customer.selectSummary |
| TRANSACTION_CODE | 거래코드 | SV-INQ-0001 |
| CALL_TYPE | 호출 유형 | PRIMARY, AUX, POPUP |
| REQUIRED_YN | 화면 필수 서비스 여부 | Y |
| AUTH_REQUIRED_YN | 기능권한 필요 여부 | Y |
| AUDIT_YN | 감사 대상 여부 | Y |
| SORT_ORDER | 호출 순서 | 1 |
| USE_YN | 사용 여부 | Y |
| DESCRIPTION | 설명 | 고객요약 조회 호출 |

### 7.4 OM_SCREEN_FUNCTION_AUTH 화면 기능권한

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| AUTH_GROUP_ID | 권한그룹 ID | SV_USER |
| SCREEN_ID | 화면번호 | SVLIST0001 |
| FUNCTION_CODE | 기능코드 | SEARCH |
| ALLOW_YN | 허용 여부 | Y |
| AUDIT_YN | 감사 대상 여부 | N |
| USE_YN | 사용 여부 | Y |

## 8. CALL_TYPE 기준

화면에서 호출하는 ServiceId의 성격을 구분한다.
CALL_TYPE
| 의미 | 설명 | PRIMARY | 대표 서비스 |
| --- | --- | --- | --- |
| 화면의 핵심 업무 서비스 | SECONDARY | 보조 서비스 | 대표 서비스 외 추가 업무 처리 |
| COMMON | 공통 서비스 | 공통코드, 권한, 환경설정 조회 | POPUP |
| 팝업 서비스 | 팝업 화면에서 호출 | TAB | 탭 서비스 |
| 탭 전환 시 호출 | FILE | 파일 서비스 | 업로드/다운로드 |
| REPORT | 리포트 서비스 | 출력/리포트 생성 | AUDIT |
| 감사 서비스 | 감사로그 기록 또는 조회 | INIT | 초기화 서비스 |

화면 진입 시 초기 데이터 조회

## 9. 화면-ServiceId 매핑 예시

### 9.1 SV 고객요약조회 화면

| 항목 | 값 |
| --- | --- |
| ScreenId | SVLIST0001 |
| 화면명 | 고객요약조회 |
| MenuId | SVMENU0002 |
| 화면경로 | /ui/sv/customer/sv-customer-list.html |

| FunctionCode | ServiceId | TransactionCode |
| --- | --- | --- |
| CALL_TYPE | 감사 | INIT |
| SV.Customer.init | SV-INQ-0000 | INIT |
| N | SEARCH | SV.Customer.selectSummary |
| SV-INQ-0001 | PRIMARY | Y |
| DETAIL | SV.Customer.selectDetail | SV-INQ-0002 |
| SECONDARY | Y | DOWNLOAD |
| SV.Customer.downloadExcel | SV-DWN-0001 | FILE |
| Y | COMMON | CC.CommonCode.selectList |
| CC-INQ-0001 | COMMON | N |

### 9.2 CM 캠페인 등록 화면

| 항목 | 값 |
| --- | --- |
| ScreenId | CMREG0001 |
| 화면명 | 캠페인 등록 |
| MenuId | CMMENU0003 |
| 화면경로 | /ui/cm/campaign/cm-campaign-register.html |

| FunctionCode | ServiceId | TransactionCode |
| --- | --- | --- |
| CALL_TYPE | 감사 | INIT |
| CM.Campaign.initCreate | CM-INQ-0000 | INIT |
| N | SAVE | CM.Campaign.create |
| CM-REG-0001 | PRIMARY | Y |
| UPLOAD | CM.Target.upload | CM-UPL-0001 |
| FILE | Y | DOWNLOAD |
| CM.Campaign.downloadTemplate | CM-DWN-0001 | FILE |
| Y | COMMON | CC.CommonCode.selectList |
| CC-INQ-0001 | COMMON | N |

### 9.3 OM 사용자 관리 화면

| 항목 | 값 |
| --- | --- |
| ScreenId | OMLIST0002 |
| 화면명 | 사용자 관리 |
| MenuId | OMMENU0003 |
| 화면경로 | /ui/om/admin/om-user-list.html |

| FunctionCode | ServiceId | TransactionCode |
| --- | --- | --- |
| CALL_TYPE | 감사 | SEARCH |
| OM.User.selectList | OM-INQ-0001 | PRIMARY |
| N | DETAIL | OM.User.selectDetail |
| OM-INQ-0002 | SECONDARY | N |
| CREATE | OM.User.create | OM-REG-0001 |
| SECONDARY | Y | UPDATE |
| OM.User.update | OM-UPD-0001 | SECONDARY |
| Y | DELETE | OM.User.delete |
| OM-DEL-0001 | SECONDARY | Y |
| DOWNLOAD | OM.User.downloadExcel | OM-DWN-0001 |

FILE
Y

## 10. 검증 로직 기준

STF 전처리에서 다음 순서로 검증한다.
## 1. Header 필수값 검증

   - businessCode
   - menuId
   - screenId
   - functionCode
   - serviceId
   - transactionCode

## 2. 화면 존재 확인

   - OM_SCREEN.SCREEN_ID 존재 여부
   - USE_YN = 'Y'

## 3. 화면-ServiceId 매핑 확인

   - OM_SCREEN_SERVICE_MAP 존재 여부
   - SCREEN_ID + FUNCTION_CODE + SERVICE_ID 일치
   - USE_YN = 'Y'

## 4. ServiceId 마스터 확인

   - OM_SERVICE_CATALOG 존재 여부
   - SERVICE_ID 활성 여부
   - TRANSACTION_CODE 일치 여부

## 5. 기능권한 확인

   - 사용자 권한그룹 기준
   - SCREEN_ID + FUNCTION_CODE 허용 여부

## 6. 거래통제 확인

   - serviceId
   - transactionCode
   - businessCode
   - userId
   - channelId
   - branchId

## 7. Timeout 정책 확인

## 8. 거래로그 PROCESSING 등록

## 11. 차단 기준

| 차단 조건 | 오류코드 예시 | 설명 |
| --- | --- | --- |
| screenId 없음 | E-TCF-HDR-0007 | 화면번호 누락 |

functionCode 없음
E-TCF-HDR-0008

| 기능코드 누락 | 미등록 화면 |
| --- | --- |
| E-OM-SCR-0001 | OM_SCREEN에 없음 |
| 사용중지 화면 | E-OM-SCR-0002 |
| USE_YN = N | 화면-ServiceId 미매핑 |
| E-OM-SCR-0003 | 화면에서 호출 불가한 ServiceId |
| ServiceId 미등록 | E-TCF-SVC-0001 |
| Service Catalog에 없음 | 거래코드 불일치 |
| E-TCF-SVC-0004 | ServiceId와 거래코드 불일치 |
| 기능권한 없음 | E-TCF-AUTHZ-0001 |
| 버튼/기능 권한 없음 | 거래통제 차단 |
| E-TCF-CTL-0001 | 거래통제 정책상 차단 |

## 12. 화면과 ServiceId 연결 정책

### 12.1 허용 정책

| 정책 | 기준 |
| --- | --- |
| 한 화면은 여러 ServiceId를 가질 수 있다 | 조회, 상세, 저장, 다운로드 등 |
| 하나의 ServiceId는 여러 화면에서 재사용 가능하다 | 공통코드조회, 지점조회 등 |
| 업무 ServiceId는 동일 업무 화면에서 호출하는 것을 원칙으로 한다 | SV 화면은 SV.* 우선 |
| 공통 ServiceId는 모든 화면에서 호출 가능하되 별도 COMMON 매핑 필요 | CC.CommonCode.selectList |
| 팝업 ServiceId는 팝업 ScreenId와 별도 매핑한다 | SVPOP0001 |
| 다운로드 ServiceId는 반드시 감사 대상이다 | AUDIT_YN = Y |
| 등록/수정/삭제 ServiceId는 반드시 기능권한 대상이다 | AUTH_REQUIRED_YN = Y |

### 12.2 제한 정책

| 제한 | 기준 |
| --- | --- |
| 화면에 매핑되지 않은 ServiceId 호출 금지 | Header 위변조 방지 |
| 다른 업무 ServiceId 직접 호출 금지 | 예: SV 화면에서 OM.User.delete 호출 금지 |
| 다운로드 ServiceId 공통 우회 호출 금지 | 반드시 화면과 기능권한 매핑 |
| 권한 없는 FunctionCode 호출 금지 | 버튼 숨김만으로는 부족 |
| 폐기 화면의 ServiceId 호출 금지 | OM_SCREEN.USE_YN = N이면 차단 |
| 테스트 ServiceId 운영 호출 금지 | USE_YN = N 또는 ENV_CODE로 차단 |

## 13. ServiceId 재사용 기준

ServiceId 재사용은 허용하되 기준을 둔다.
| 유형 | 재사용 가능 여부 | 기준 | 공통코드 조회 |
| --- | --- | --- | --- |
| 가능 | CC.CommonCode.selectList | 지점 조회 | 가능 |
| CC.Branch.selectList | 사용자 기본정보 조회 | 제한적 가능 | 개인정보 여부 확인 |
| 고객 상세 조회 | 원칙적으로 제한 | 화면별 감사 필요 | 다운로드 |
| 제한 | 화면별 감사/권한 필요 | 등록/수정/삭제 | 제한 |
| 화면별 기능권한 필요 | OM 관리 기능 | 제한 | OM 화면에서만 호출 |

## 14. 거래로그 연계 기준

화면-ServiceId 검증 후 거래로그에는 다음 항목을 남긴다.
| 로그 항목 | 설명 |
| --- | --- |
| 예시 | GUID |
| 거래 고유 ID | 7f9c... |
| TRACE_ID | 추적 ID |
| TRC202607050001 | MENU_ID |
| 메뉴 ID | SVMENU0002 |
| SCREEN_ID | 화면번호 |
| SVLIST0001 | FUNCTION_CODE |
| 기능코드 | SEARCH |
| SERVICE_ID | ServiceId |
| SV.Customer.selectSummary | TRANSACTION_CODE |
| 거래코드 | SV-INQ-0001 |
| BUSINESS_CODE | 업무코드 |
| SV | USER_ID |
| 사용자 | U123456 |
| BRANCH_ID | 지점 |
| 001234 | CHANNEL_ID |
| 채널 | WEBTOP |
| RESULT_STATUS | 처리상태 |
| SUCCESS | ERROR_CODE |
| 오류코드 | NULL |
| ELAPSED_MS | 처리시간 |
| 312 |  |

## 15. 감사로그 연계 기준

다음 조건이면 화면-ServiceId 매핑의 AUDIT_YN을 Y로 둔다.

| 조건 | 감사 여부 | 고객정보 상세조회 |
| --- | --- | --- |
| Y | 고객정보 대량조회 | Y |

| 엑셀 다운로드 | Y | 파일 다운로드 |
| --- | --- | --- |
| Y | 캠페인 대상 추출 | Y |

| 사용자 권한 변경 | Y | 메뉴권한 변경 |
| --- | --- | --- |
| Y | ServiceId 등록/수정/중지 | Y |

| 거래통제 변경 | Y | Timeout 정책 변경 |
| --- | --- | --- |
| Y | 단순 공통코드 조회 | N |

화면 초기화용 코드조회
N

## 16. DDL 예시

CREATE TABLE OM_SCREEN_SERVICE_MAP (
    MAP_ID              VARCHAR2(30)  NOT NULL,
    SCREEN_ID           VARCHAR2(20)  NOT NULL,
    FUNCTION_CODE       VARCHAR2(30)  NOT NULL,
    SERVICE_ID          VARCHAR2(100) NOT NULL,
    TRANSACTION_CODE    VARCHAR2(30)  NOT NULL,
    CALL_TYPE           VARCHAR2(20)  NOT NULL,
    REQUIRED_YN         CHAR(1)       DEFAULT 'N' NOT NULL,
    AUTH_REQUIRED_YN    CHAR(1)       DEFAULT 'Y' NOT NULL,
    AUDIT_YN            CHAR(1)       DEFAULT 'N' NOT NULL,
    SORT_ORDER          NUMBER(5)     DEFAULT 1 NOT NULL,
    USE_YN              CHAR(1)       DEFAULT 'Y' NOT NULL,
    DESCRIPTION         VARCHAR2(500),
    CREATED_BY          VARCHAR2(50)  NOT NULL,
    CREATED_DTM         DATE          DEFAULT SYSDATE NOT NULL,
    UPDATED_BY          VARCHAR2(50),
    UPDATED_DTM         DATE,
    CONSTRAINT PK_OM_SCREEN_SERVICE_MAP
        PRIMARY KEY (MAP_ID),
    CONSTRAINT UK_OM_SCREEN_SERVICE_MAP_01
        UNIQUE (SCREEN_ID, FUNCTION_CODE, SERVICE_ID)
);

권장 인덱스는 다음과 같다.
CREATE INDEX IDX_OM_SCREEN_SERVICE_MAP_01
    ON OM_SCREEN_SERVICE_MAP (SCREEN_ID, FUNCTION_CODE);

CREATE INDEX IDX_OM_SCREEN_SERVICE_MAP_02
    ON OM_SCREEN_SERVICE_MAP (SERVICE_ID);

CREATE INDEX IDX_OM_SCREEN_SERVICE_MAP_03
    ON OM_SCREEN_SERVICE_MAP (TRANSACTION_CODE);

## 17. 표준 전문 예시

```json
{
  "header": {
    "guid": "7f9c9d10-1111-2222-3333-111122223333",
    "traceId": "TRC202607050001",
    "businessCode": "SV",
    "menuId": "SVMENU0002",
    "screenId": "SVLIST0001",
    "functionCode": "SEARCH",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "userId": "U123456",
    "branchId": "001234",
    "channelId": "WEBTOP"
  },
  "body": {
    "customerNo": "CUST000001"
  }
}
```

이 요청은 다음 기준을 모두 통과해야 한다.
| 검증 항목 | 기준 |
| --- | --- |
| SVLIST0001 | 등록된 화면인가 |
| SEARCH | 화면에서 허용된 기능인가 |
| SV.Customer.selectSummary | 화면에 매핑된 ServiceId인가 |
| SV-INQ-0001 | ServiceId의 거래코드와 일치하는가 |
| U123456 | 해당 기능권한이 있는가 |
| SV | 화면 업무코드와 ServiceId 업무코드가 일치하는가 |
| 거래통제 | 사용자/채널/지점 기준 허용 거래인가 |

## 18. 개발 적용 예시

업무 개발자는 화면에서 ServiceId를 직접 하드코딩하기보다, 화면별 서비스 매핑 정보를 기준으로 호출한다.
const request = {
  header: {
    businessCode: "SV",
    menuId: "SVMENU0002",
    screenId: "SVLIST0001",
    functionCode: "SEARCH",
    serviceId: "SV.Customer.selectSummary",
    transactionCode: "SV-INQ-0001"
  },
  body: {
    customerNo: customerNo
  }
};

서버에서는 다음처럼 검증한다.
screenServiceValidator.validate(
    header.getScreenId(),
    header.getFunctionCode(),
    header.getServiceId(),
    header.getTransactionCode()
);

## 19. 운영관리 화면 기준

OM에는 다음 관리 화면을 둔다.

| OM 화면 | 설명 | 화면 마스터 관리 |
| --- | --- | --- |
| OM_SCREEN 등록·수정 | 화면-ServiceId 매핑 관리 | OM_SCREEN_SERVICE_MAP 등록·수정 |
| 화면 기능권한 관리 | OM_SCREEN_FUNCTION_AUTH 관리 | ServiceId 카탈로그 관리 |

OM_SERVICE_CATALOG 관리
거래통제 관리
TCF_TRANSACTION_CONTROL 관리
감사대상 서비스 관리
AUDIT_YN 관리

## 20. 검토 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| 모든 화면에 SCREEN_ID가 부여되어 있는가? | □ |
| 화면에서 호출하는 모든 ServiceId가 OM_SCREEN_SERVICE_MAP에 등록되어 있는가? | □ |
| SCREEN_ID + FUNCTION_CODE + SERVICE_ID가 유일한가? | □ |
| ServiceId와 TransactionCode가 일치하는가? | □ |
| 화면 업무코드와 ServiceId 업무코드가 원칙적으로 일치하는가? | □ |
| 공통 ServiceId는 CALL_TYPE = COMMON으로 등록되어 있는가? | □ |
| 다운로드/대량조회/권한변경 서비스는 AUDIT_YN = Y인가? | □ |
| 등록/수정/삭제 서비스는 AUTH_REQUIRED_YN = Y인가? | □ |
| 폐기 화면의 매핑이 비활성화되어 있는가? | □ |
| 거래로그에 screenId, functionCode, serviceId가 남는가? | □ |
| Header 위변조 시 차단되는가? | □ |
| OM 관리 화면에서 매핑 변경 이력이 남는가? | □ |

## 21. 마무리말

화면번호와 ServiceId 연결 기준의 핵심은 다음과 같다.
화면은 사용자가 보는 단위이고,
ServiceId는 서버가 실행하는 단위이며,
FunctionCode는 사용자가 누른 기능 단위다.

따라서 NSIGHT에서는 단순히 “화면 하나에 ServiceId 하나”로 설계하면 안 된다.정확한 기준은 다음이다.
SCREEN_ID + FUNCTION_CODE + SERVICE_ID + TRANSACTION_CODE

이 네 가지를 OM에서 매핑하고, TCF 전처리에서 검증해야 한다.이 구조를 적용하면 화면에서 허용되지 않은 ServiceId 호출을 차단할 수 있고, 거래로그와 감사로그에서 어느 화면의 어떤 버튼이 어떤 ServiceId를 실행했는지 명확히 추적할 수 있다.

---

## 관련 Manual 장

- [16장](./16-ServiceId-설계.md)
- [21장](./21-Header-작성-기준.md)

## 원본

- [`znsight-guide-word`](../znsight-guide-word/) — `명명규칙 상세 (17).docx`
