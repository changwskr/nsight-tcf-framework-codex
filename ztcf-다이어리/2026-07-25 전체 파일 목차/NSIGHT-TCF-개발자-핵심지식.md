# NSIGHT TCF — 개발자가 알아야 할 핵심 (목차 A~J 종합)

생성일: 2026-07-25  
기준 목차: [NSIGHT-TCF-전체문서-읽기순서.md](./NSIGHT-TCF-전체문서-읽기순서.md)  
범위: Markdown 지식 종합 (**Word 제외**)

이 문서는 저장소 문서가 많다는 전제에서, **목차 구간(A~J)을 전부 훑은 뒤 업무 개발자가 실무에서 반드시 알아야 할 내용만** 한 권으로 재구성한 것입니다.  
파일별 상세 발췌는 [목차순서별-핵심내용](./NSIGHT-TCF-목차순서별-핵심내용.md)을 참고하세요.

---

## 0. 한 장으로 암기할 것

```text
POST /{업무코드}/online  +  header.serviceId
        │
        ▼
   tcf-web → TCF.process()
        │
   STF (검증·세션·통제·Timeout·로그 시작)
        │
   Dispatcher (serviceId → Handler)
        │
   Handler → Facade → Service → Rule → DAO → Mapper
        │
   ETF (표준 응답·오류코드·로그 종료)
```

| 원칙 | 내용 |
|------|------|
| URL | 기능마다 API를 만들지 않는다. 입구는 `/online` 하나 |
| 기능 키 | `serviceId` (예: `SV.Customer.selectSummary`) |
| Handler | **도메인당 1개** + `serviceIds()` + `switch` |
| 계층 | Handler는 Facade만. SQL은 Mapper. 트랜잭션은 Facade |
| WAR 간 호출 | Java import 금지 → **tcf-eai** |
| 등록 | OM Catalog + 거래통제 + Timeout |
| 설계서 주의 | 옛 설계 “serviceId당 Handler 1” ≠ **현재 코드** |

스택: **Java 21 · Spring Boot 3.3 · Gradle · MyBatis · (로컬) H2 Oracle MODE**

---

## A. 입문에서 익힐 것 (`ztcfbook-m`)

### A.1 TCF vs REST

| | 일반 REST | NSIGHT TCF |
|---|-----------|------------|
| 주소 | 기능마다 다름 | `POST /{업무코드}/online` |
| 기능 구분 | URL | **serviceId** |
| 로그인·검사 | API마다 구현 | **STF가 공통 처리** |

> 업무 코드는 개발자가 짜고, “거래를 어떻게 실행·기록할지”는 TCF가 맡는다.

### A.2 처음 3가지만

1. 요청·응답 = 표준 JSON (`header` + `body`, 응답은 `header` + `result` + `body`)
2. 기능은 **serviceId**로 찾는다
3. Handler는 **Facade만** 호출 (SQL·긴 업무 if 금지)

### A.3 6계층 책임

| 계층 | 역할 | 금지 |
|------|------|------|
| **Handler** | serviceId 분기, Body↔DTO, Facade 호출 | SQL, `@Transactional`, Header/세션 |
| **Facade** | 유스케이스 조립, **`@Transactional`** | SQL 직접 |
| **Service** | 업무 순서·조합 | HTTP·세션 |
| **Rule** | 검증·업무 규칙·오류코드 | DB 연결 |
| **DAO** | Mapper 호출·영속 예외 | 업무 if 가득 |
| **Mapper** | SQL (XML) | — |

### A.4 이름

- ServiceId: `{업무}.{도메인}.{행동}` → `SV.Customer.selectSummary`
- 거래코드: `{업무}-{유형}-{번호}` → `SV-INQ-0001` (통제·감사용)
- Handler: `{업무}{도메인}Handler` → `SvCustomerHandler`
- 패키지: `com.nh.nsight.{업무소문자}.entry|application|persistence|…`

자주 쓰는 행동: `selectList`, `selectSummary`/`selectOne`, `register`, `update`, `delete`

### A.5 실습 경로

1. `sv-service` bootRun (8086, `/sv`)
2. 샘플 JSON으로 `POST /sv/online` (고객요약)
3. 목록·등록은 같은 6계층 패턴 복제
4. 완료 전: 부록 H 체크 (serviceId·OM·curl SUCCESS·build·마스킹)

---

## B. 아키텍처에서 익힐 것 (`zarchitecture`)

### B.1 End-to-End

```text
Client/UI → GSLB·L4·Apache → tcf-gateway(선택)
  → 업무 WAR(/sv…) 또는 tcf-om(/om)
  → tcf-web → tcf-core(TCF)
  → 6계층 → DB(RDW/OMDB/LOG 등)
```

### B.2 TCF 엔진 3모듈

| 모듈 | 역할 |
|------|------|
| tcf-util | Spring 비의존 유틸 |
| tcf-core | STF/TCF/ETF, Dispatcher, Timeout, 거래통제, 표준 전문 |
| tcf-web | HTTP `/online`, AutoConfiguration, 예외→표준응답 |

의존: `util ← core ← web ← 업무 WAR`

### B.3 STF가 대신 해주는 것 (개발자가 다시 하지 말 것)

Header 필수검증 · GUID/TraceId · 세션·인증·권한 · 거래통제 · Idempotency · Timeout 정책 조회 · 거래로그 PROCESSING 시작

### B.4 업무 WAR

| 코드 | Context | 포트(로컬) | 비고 |
|------|---------|-----------|------|
| IC | /ic | 8082 | |
| PC | /pc | 8083 | |
| MS | /ms | 8085 | |
| **SV** | /sv | **8086** | 입문 실습 추천 |
| PD | /pd | 8087 | |
| EB | /eb | 8089 | 이벤트 브리지 |
| EP | /ep | 8090 | 이벤트 처리 |
| SS | /ss | 8093 | |
| MG | /mg | 8096 | |
| OM | /om | 8097 | **tcf-om** (레거시 om-service와 충돌 주의) |

같은 이름(Customer)이라도 **SV vs IC는 다른 도메인·다른 WAR**.

### B.5 플랫폼 모듈 (개발자가 자주 만지는 경계)

| 모듈 | 포트 | 개발자 관점 |
|------|------|-------------|
| tcf-gateway | 8100 | 라우팅. 업무 WAR에 Controller 추가하지 말 것 |
| tcf-jwt | 8110 | 토큰 발급·검증 영역 |
| tcf-om | 8097 | Catalog·통제·운영 기준정보 |
| tcf-ui / tcf-uj | 8099 / 8102 | 로컬·Gateway 동형 테스트 UI |
| tcf-eai | JAR | **WAR 간 호출 유일한 길** |
| tcf-cache | JAR | 기준정보 캐시 |
| tcf-batch | 8098 | 배치·스케줄 (온라인 6계층과 분리) |

NFR(설계 목표 감각): 동시 사용자 수만 명대 · TPS 수백~수천 · P95 3초 · 가용성 높게.

---

## C. 설계서(`zman`)에서 익힐 것 — 특히 “코드와 다른 점”

### C.1 불변 결론

> 모든 온라인 거래 = **`TCF.process()` 단일 진입**  
> STF → TimeoutExecutor → Dispatcher → Handler → ETF

### C.2 Handler (현재 코드 기준)

```java
@Component
public class SvCustomerHandler implements TransactionHandler {
    @Override
    public Collection<String> serviceIds() {
        return List.of("SV.Customer.selectSummary", "SV.Customer.selectList");
    }
    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        return switch (context.getHeader().getServiceId()) {
            case "SV.Customer.selectSummary" -> facade.selectCustomerSummary(request.getBody(), context);
            case "SV.Customer.selectList" -> facade.selectCustomerList(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND, ...);
        };
    }
}
```

| ✅ | ❌ |
|----|-----|
| serviceId 진입, DTO 변환, Facade 호출 | Header/세션/통제, SQL, StandardResponse 조립 |

### C.3 설계서 vs 코드 (필수)

| 주제 | 설계서(docx 구 관점) | **따라야 할 코드** |
|------|----------------------|-------------------|
| Handler | serviceId당 1 클래스 | **도메인당 1 Handler** |
| OM Handler 수 | 매우 많음 | 소수 도메인 Handler |
| EAI | 원칙 | **tcf-eai 모듈** |

일치하는 것: StandardRequest/Response, `/online`, STF→Dispatcher→ETF, 6계층 패키지 구조.

→ 상세: [zman/00-설계서-코드베이스-대조표.md](../../zman/00-설계서-코드베이스-대조표.md)

### C.4 표준 전문 (요청·응답)

**요청**

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "guid": "",
    "traceId": "",
    "requestTime": "2026-06-15T10:30:00+09:00"
  },
  "body": { "customerNo": "A001" }
}
```

**응답:** `header` + `result`(resultCode/errorCode/message…) + `body`  
페이징은 보통 `body.page` + `body.list` (TCF 코어 비관여 → Rule/DAO)

오류 유형 감각: Header `E-TCF-HDR-*` · 통제 `E-TCF-CTL-*` · Timeout `E-TCF-TIME-*` · 업무 `BusinessException`

---

## D. 개념·소스 인덱스에서 익힐 것 (`zdocs-2` / `zdocs-1`)

### D.1 용어

| 약어 | 의미 |
|------|------|
| **STF** | 전처리 (검증·세션·통제·Timeout·TX_START) |
| **TCF** | 오케스트레이션 (`process`) |
| **BTF** | 업무 계층 (Handler 이후 6계층) |
| **ETF** | 후처리 (응답·감사·메트릭·TX_END) |

### D.2 프레임워크 vs 어플리케이션

- **프레임워크(tcf-web/core):** HTTP 진입, STF, Dispatcher, ETF  
- **어플리케이션(*-service):** entry / application / persistence  
- 계약만 의존: `StandardRequest`, `TransactionContext`, `TransactionHandler`

### D.3 찾을 때

- 소스 위치 → `zdocs-1/SOURCE_INDEX.md`
- Handler 표준 코드 → `TCF_FRAMEWORK_GUIDE.md`
- 주제 심화(세션·Timeout·DAO·캐시·배치·페이징·명명) → `zdocs-2/*.md`
- 상세 아키텍처 정의 → `zdocs-1/architecture/`

---

## E. 모듈 가이드에서 익힐 것 (`zguide` + `znsight-man` MD)

> Word 개발 매뉴얼은 제외. 실무 규칙은 아래와 동일.

### E.1 공통 규칙 (한 페이지)

1. **Controller 만들지 않음** — `/online`은 tcf-web
2. **Handler = 도메인당 1**
3. **WAR 간 호출 = tcf-eai만**
4. **serviceId는 OM Catalog + 거래통제에 등록** 후에야 운영 거래
5. 로컬: `gradle :{module}:bootRun` 또는 `tcf-scripts`

### E.2 패키지 표준 (`znsight-man` 요지)

```text
com.nh.nsight.{업무코드소문자}
 ├─ entry/handler, facade
 ├─ application/service, rule
 ├─ persistence/dao, mapper
 ├─ dto/request, response, command, result
 ├─ client          ← tcf-eai 클라이언트
 ├─ support, config, exception, constant
```

패키지 = 계층 경계 = 리뷰·테스트 범위의 기준.

### E.3 SV로 시작하는 법

```bash
gradle :sv-service:bootRun
# POST http://127.0.0.1:8086/sv/online
# 샘플: tcf-ui/.../sample-requests/
# UI: http://localhost:8099 (tcf-ui)
```

담당 WAR 가이드: `zguide/{코드}-service-개발가이드.md`  
플랫폼: `tcf-core` → 담당 WAR → 필요 시 `tcf-eai` → `tcf-ui`

---

## F. 심화 집필에서 익힐 것 (`ztcf-집필본-md` / `ztcfbook`)

입문(A) 이후 개발자가 채워야 할 **실무 깊이**:

| 주제 | 알아야 할 것 |
|------|-------------|
| 화면→소스 | screen/event → serviceId → Handler 위치 추적 |
| 첫 조회 | DTO·Service·DAO·Mapper까지 한 거래 완성 |
| 목록·등록 | page/list, processingType, idempotencyKey, `@Transactional` |
| 수정·삭제·상태 | 상태전이·동시성·트랜잭션 범위 |
| 오류 | 업무 오류코드 vs TCF 오류, 재시도 가능 여부 |
| 인증 | 세션·JWT·Gateway — 업무 코드에 재구현 금지 |
| 연동 | 내부 EAI / 외부 HTTP — timeout·실패 정책 |
| 품질 | 단위·통합·리뷰 Quality Gate·CI/CD |
| 운영 | 로그(guid)·메트릭·캐시·배치 경계 |

Word 집필본은 제외하고 **MD/책 본문**을 기준으로 한다.

---

## G. 장 단위 표준에서 익힐 것 (`znsight-man`)

개발 매뉴얼 MD의 “현장 규칙” 압축:

- **명명·패키지·계층**은 취향이 아니라 표준 (리뷰 반려 사유)
- Header·DTO·Validation·Endpoint·SQL 작성 규칙은 **거래마다 동일 패턴**
- Timeout·거래통제·감사는 **OM/STF 영역** — 업무 Handler에 우회 구현 금지
- 신규 거래 완료 조건 = 코드 + OM 등록 + 테스트 + (필요 시) 설계 산출물

---

## H. 모델 우선·자동화에서 익힐 것 (`ai-방법론.md`)

### H.1 원칙

1. **Java부터 쓰지 않는다.** 화면·ServiceId·테이블·DTO·Rule·SQL을 먼저 모델로 확정  
2. 자동화(Model Studio) = **골격·초안** / 사람 = **업무 판단·SQL 성능·오류코드·트랜잭션**  
3. 생성 성공 ≠ 완료. **Compile · Test · Review · OM · 배포**까지

### H.2 0~19 단계 (개발자 시점)

| 구간 | 단계 | 개발자가 할 일 |
|------|------|----------------|
| 모델링 | 0~8 | 요구사항→도메인→테이블→화면→ServiceId→DTO/Rule/SQL 정의 |
| 검증·생성 | 9~10 | 통합검증(오류 0) 후 ZIP/코드 생성 |
| 구현 | 11~12 | Git 병합, Service/Rule/SQL **업무 로직 보완** |
| 시험 | 13~15 | 단위·TCF 통합·품질/보안 |
| 이관 | 16~19 | OM·산출물, PR, 배포, 변경관리 |

### H.3 추적성 (빠지면 리뷰에서 막힘)

```text
화면·이벤트 → ServiceId·거래코드 → Handler/6계층 → Mapper/SQL → 테이블
                 ↘ OM Catalog · Timeout · 감사
```

---

## I. 용량·설정에서 개발자가 알 것 (`ztcf-book-capacity-md` 등)

아키텍트/인프라 문서가 많지만, **개발자가 깨면 안 되는 감각**:

1. **Timeout은 다층** — Online Timeout과 Query Timeout을 멋대로 키우면 Pool·스레드가 먼저 고갈  
2. **Hikari Pool · Tomcat thread · Heap**는 한 세트 — 로컬에서 무제한으로 키운 설정을 운영에 들고 가지 말 것  
3. 용량 체인: 사용자 → 세션 → TPS → AP 대수 → DB Pool 총량 → 장애 시 잔여량  
4. 기준 감각(문서 예): TPS 360/720/1080, P95 3초, VM 프로필(8C/32G 등)  
5. 배포 단위는 **WAR**. `tcf-core`/`web`은 WAR 안에 포함

상세 수치·프로파일은 `ztcf-book-capacity-md`를 보고, Word 용량 문서는 제외.

---

## J. 다이어리에서 얻을 것

표준이 아니라 **결정 맥락**(왜 Model Studio인지, 도메인 코드 정의 등).  
일상 개발 규칙의 근거는 A~I, 다이어리는 보조.

---

## 신규 거래 — 개발자 체크리스트

### 설계·모델

- [ ] 업무코드(WAR)·도메인·ServiceId·거래코드 확정
- [ ] 화면 이벤트 ↔ ServiceId 연결
- [ ] 테이블·컬럼·PK / 요청·응답 필드 / Rule·오류코드
- [ ] Timeout·감사·멱등 필요 여부

### 구현

- [ ] 기존 도메인 Handler에 serviceId 추가 (또는 신규 도메인 Handler 1개)
- [ ] Facade `@Transactional` 범위 올바름 (조회 readOnly 등)
- [ ] Service → Rule → DAO → Mapper XML만 SQL
- [ ] 다른 WAR 호출은 tcf-eai
- [ ] 개인정보 로그 마스킹

### 검증·이관

- [ ] `gradlew build` 통과
- [ ] curl/tcf-ui로 SUCCESS + 필수값 오류 케이스
- [ ] OM Catalog·거래통제·Timeout 등록
- [ ] PR 리뷰 · (운영) Smoke

---

## 역할별 최소 학습 경로

| 역할 | 이 문서에서 우선 |
|------|------------------|
| **신규 업무 개발** | 0 → A → B.2~B.4 → C → E → H → 체크리스트 |
| **플랫폼/공통** | B 전체 → C → D → E(tcf-*) → I |
| **리뷰어** | C.3 차이표 · A.3 계층 금지 · E.1 · 체크리스트 |
| **자동화 사용** | H 전체 + Model Studio UI + A/C 명명 |

---

## 관련 문서

| 문서 | 용도 |
|------|------|
| [전체문서-읽기순서](./NSIGHT-TCF-전체문서-읽기순서.md) | 파일 링크 목차 |
| [목차순서별-핵심내용](./NSIGHT-TCF-목차순서별-핵심내용.md) | 파일 1:1 풍부 발췌 |
| [읽기순서별-요약](./NSIGHT-TCF-읽기순서별-요약.md) | 구간별 짧은 요약 |
| [전체문서-핵심요약](./NSIGHT-TCF-전체문서-핵심요약.md) | 주제별 압축 |

---

## 한 문장 결론

> NSIGHT 업무 개발자는 **`/online` + serviceId**로 들어가, **도메인 Handler와 6계층**만 구현하고, **STF/ETF·OM 통제는 프레임워크에 맡기며**, 가능하면 **모델→생성→로직 보완→테스트→OM** 순으로 거래를 완성한다.
