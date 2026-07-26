# 업무도메인 개발지시 샘플 프롬프트 모음

`2026-07-26-업무도메인-개발지시-프롬프트.md`(마스터 지시서)를 기반으로,
거래 유형·시나리오별로 바로 사용할 수 있게 파라미터를 채워 넣은 샘플 세트입니다.

각 샘플의 코드 블록 전문을 Cursor 채팅에 그대로 붙여넣으면 실행됩니다.

## 샘플 목록

| 파일 | 시나리오 | 업무코드 | ServiceId 예 | 처리유형 |
|---|---|---|---|---|
| `샘플1-LN-신규모듈-단건조회.md` | 신규 모듈 생성 + 단건 조회 | LN | `LN.Loan.inquiry` | INQUIRY |
| `샘플2-AV-도메인추가-목록조회.md` | **기존 모듈**에 도메인 추가 + 페이징 목록 조회 | AV | `AV.AssetValuation.inquiryList` | INQUIRY |
| `샘플3-DP-등록거래-CREATE.md` | 신규 모듈 생성 + 등록 거래 | DP | `DP.Deposit.create` | CREATE |
| `샘플4-CD-수정삭제-UPDATE-DELETE.md` | 신규 모듈 생성 + 수정/삭제 2거래 | CD | `CD.Card.update` / `CD.Card.delete` | UPDATE/DELETE |
| `샘플5-FX-외부연동조회-CLIENT.md` | 신규 모듈 + client 계층 외부 연동 조회 | FX | `FX.ExchangeRate.inquiry` | INQUIRY |
| `_TEMPLATE-업무도메인-개발지시.md` | 빈 플레이스홀더 템플릿 | — | — | — |

## 공통 주의사항 (모든 샘플에 이미 반영됨)

### 1. 포트 예약 현황

`tcf-ui/src/main/java/com/nh/nsight/tcf/ui/support/BusinessModuleDefinitions.java`가
릴레이 포트의 단일 기준(SoT)입니다. 아래는 이미 예약된 포트입니다.

```text
8081 CC   8082 IC   8083 PC   8084 BC   8085 MS   8086 SV   8087 PD
8088 CM   8089 EB   8090 EP   8091 BP   8092 BD   8093 SS   8094 OC/CS
8095 CT   8096 MG   8097 OM/UD  8099 tcf-ui  8100 tcf-gateway
8101 AV   8110 JWT
```

샘플의 권장 포트(8103~8106)는 이 목록과 충돌하지 않는 임시값이며,
구현 전 저장소 전체(`application-*.yml`, Gateway Route, 실행 스크립트)에서
충돌 여부를 재확인해야 합니다.

### 2. tcf-ui 릴레이 등록 (누락 시 화면·릴레이 불가)

신규 업무코드는 반드시 두 가지를 함께 반영해야 합니다.

1. `BusinessModuleDefinitions.ALL`에 `ModuleDefinition("{코드}", "{이름}", "{그룹}", {포트})` 추가
2. `tcf-ui/src/main/resources/sample-requests/{코드소문자}-sample-inquiry.json` 생성
   — `BusinessModuleCatalog`가 이 파일명을 강제 로드하며, **없으면 tcf-ui 기동 자체가 실패**합니다.

### 3. 표준 전문 Header는 전체 필드 필수

`systemId, businessCode, serviceId, transactionCode, processingType, guid,
channelId, userId, branchId, requestTime, systemDate, bizDate, clientIp`
— `eb-sample-inquiry.json`을 기준으로 전부 채워야 TCF 헤더 검증을 통과합니다.

### 4. bootRun 호출 경로

bootRun의 context-path는 `/`이므로:

- 거래 호출: `POST /online` 또는 `POST /{코드소문자}/online` (둘 다 매핑됨)
- Health: `GET /actuator/health` (`/{코드소문자}/actuator/health` 아님)
