# NSIGHT TCF 전체 문서 — 핵심 요약

생성일: 2026-07-25  
기준 목차: [NSIGHT-TCF-전체문서-읽기순서.md](./NSIGHT-TCF-전체문서-읽기순서.md)

이 문서는 약 1,200개 문서 링크 목차에서 **반드시 기억할 핵심만** 압축한 요약입니다.  
상세·링크는 목차의 A~J 경로를 따르세요.

---

## 1. 한 장으로 보는 NSIGHT TCF

| 항목 | 요약 |
|------|------|
| 정의 | 마케팅 업무를 **표준 HTTP/JSON 전문**으로 처리하는 Transaction Control Framework |
| 스택 | Java 21 · Spring Boot 3.3 · Gradle 멀티모듈 · MyBatis · (로컬) H2 Oracle MODE |
| 진입점 | `POST /{업무코드}/online` — Controller를 업무별로 만들지 않음 |
| 엔진 | `TCF.process()` = **STF → Dispatcher → BTF(업무) → ETF** |
| 확장 단위 | `header.serviceId` + **도메인 Handler** (`serviceIds()` + switch) |
| 배포 | 로컬 `bootRun`(포트 분리) / 통합 `ztomcat`(게이트웨이) |

```text
Client / UI
  → GSLB · L4 · Apache
  → tcf-gateway (선택)
  → 업무 WAR (/sv, /ic, …) 또는 OM (/om)
  → tcf-web → tcf-core (TCF)
  → Handler → Facade → Service → Rule → DAO → Mapper → DB
```

**비기능 목표(설계 기준):** 동시 사용자 36,000 · TPS 720 · P95 3초 · 가용성 99.99%

---

## 2. 문서 계층이 말하는 것 (A~J)

| 경로 | 폴더 | 한 줄 요약 |
|:---:|------|-----------|
| A | `ztcfbook-m` | 초보용 쉬운 입문·실습(SV 고객요약 따라하기) |
| B | `zarchitecture` | 영역별 아키텍처(전체→TCF→6계층→WAR→OM·Gateway…) |
| C | `zman` | 설계서 1~25장 요약 + **설계↔코드 차이** |
| D | `zdocs-2` / `zdocs-1` | 주제 노트 · 소스 인덱스 · 프레임워크 가이드 |
| E | `znsight-guide-word` / `zguide` | 개발 매뉴얼(Word) · 모듈별 개발 가이드 |
| F | 집필본 / `ztcfbook*` | 심화 서술·아키텍트판·통합 개발북 |
| G | `znsight-man` | 장 단위 개발 입문서(대량) |
| H | 구축방법론 · AI 방법론 | 구축 절차 · **모델 우선 자동화(0~19단계)** |
| I | 용량·설정·엔진 | TPS/TPMC · Apache/Tomcat/JVM/Hikari · 환경값 |
| J | `ztcf-다이어리` | 일자별 작업·의사결정 메모 |

**실무 빠른 경로:** A → B(01~04) → E(담당 WAR+tcf-core) → C(07~08) → I(필요 시)  
**자동화 개발:** H의 `ai-방법론.md` + Model Studio(`tcf-ai-methodology`)

---

## 3. TCF 처리 파이프라인 (필수)

| 약어 | 의미 | 담당 |
|------|------|------|
| **STF** | Standard Transaction Front | Header 검증·세션·거래통제·Timeout·멱등·TX_START |
| **Dispatcher** | serviceId 라우팅 | `TransactionHandler` 선택 |
| **BTF** | 업무 계층 | Handler → Facade → Service → Rule → DAO |
| **ETF** | End Transaction Framework | 응답코드·감사·메트릭·TX_END |

전문 계약:

- 요청: `StandardRequest { header, body }`
- 응답: `StandardResponse { header, result, body }`
- 성공/실패 코드 체계는 ETF·오류코드 표준을 따름 (`S0000` 등)

개발자가 **하지 않는 것:** `/online` Controller, Header·세션·거래통제·응답 조립을 업무 코드에서 중복 구현.

---

## 4. 애플리케이션 6계층 (필수)

```text
Handler  →  serviceId 분기, Body↔DTO, Facade 호출
Facade   →  @Transactional 경계
Service  →  업무 오케스트레이션
Rule     →  업무 검증·오류코드
DAO      →  데이터 접근 API
Mapper   →  MyBatis SQL
```

| 규칙 | 내용 |
|------|------|
| Handler 단위 | **도메인당 1개** (`SvCustomerHandler`) — 설계서의 “serviceId당 1클래스”와 **코드가 다름** |
| 금지 | Handler→DAO 직행, Service→Mapper 직행, 다른 WAR Java import |
| WAR 간 호출 | **tcf-eai** (`TcfServiceClient`)만 |
| 등록 | OM Catalog(ServiceId) + 거래통제·Timeout |

---

## 5. 모듈·WAR 지도 (요약)

### 플랫폼

| 모듈 | 역할 | 대표 포트 |
|------|------|-----------|
| tcf-core / tcf-web / tcf-util | TCF 엔진·공통 | (JAR) |
| tcf-gateway | API Gateway | 8100 |
| tcf-jwt | JWT/SSO | 8110 |
| tcf-om | 운영관리(Catalog·통제·로그 UI 연계) | 8097 |
| tcf-ui / tcf-uj | 거래·OM UI | 8099 / 8102 |
| tcf-eai | 서비스 간 연동 | (JAR) |
| tcf-cache | 기준정보 캐시 | (JAR) |
| tcf-batch | 배치·스케줄 | 8098 |
| tcf-cicd / tcf-scripts | 배포·로컬 스크립트 | — |

### 업무 WAR (예)

| 코드 | Context | 가이드 |
|------|---------|--------|
| SV | /sv | Single View |
| IC | /ic | … |
| EB / EP | /eb /ep | 이벤트·연계 |
| PC / PD / MS / SS / MG | 각 context | 해당 `zguide/*-개발가이드.md` |

업무코드 = WAR 경계. 같은 이름(Customer)이라도 **SV vs IC는 다른 도메인**.

---

## 6. 식별자·추적성 (개발 표준의 뼈대)

한 거래를 끝까지 잇는 키:

```text
화면ID · 이벤트
  → ServiceId (예: SV.Customer.selectSummary)
  → 거래코드
  → Handler / Facade / Service / Rule
  → DAO · Mapper · SQL
  → 테이블 · 컬럼
  → OM Catalog · Timeout · 감사
```

이름 규칙 요지:

- ServiceId: `{업무}.{도메인}.{메서드}`
- Handler: `{업무}{도메인}Handler`
- 패키지: entry(handler/facade) · application(service/rule/dto) · persistence(dao/mapper)

상세 표: `ztcfbook-m` 부록 A~C, M / `zdocs-2` 명명·applicationNaming.

---

## 7. 설계서 vs 코드 (꼭 아는 차이)

| 주제 | 설계서(docx) | 현재 코드 |
|------|--------------|-----------|
| Handler | serviceId당 1 클래스 | **도메인당 1 Handler** + switch |
| OM Handler 수 | 다수(문서상 83 등) | **소수 도메인 Handler**(약 24) |
| EAI | 원칙 | **tcf-eai 모듈**로 구현 |
| SV 샘플 | selectSummary | 동일 패턴 유지 |

→ 학습 시 `zman`만 보지 말고 **[00-설계서-코드베이스-대조표](../../zman/00-설계서-코드베이스-대조표.md)** 를 함께 본다.

---

## 8. 모델 우선 · AI 자동화 (H 경로 요약)

원칙: **코드 먼저 쓰지 않는다. 모델 → 검증 → 생성 → 업무 로직 보완 → 테스트 → OM → 배포.**

자동화(Model Studio)가 주는 것 = **골격·초안**  
개발자가 채우는 것 = **업무 판단·SQL 성능·오류코드·트랜잭션 범위·테스트 시나리오**

단계 압축(0~19):

| 구간 | 단계 | 내용 |
|------|------|------|
| 준비·모델링 | 0~8 | 요구사항 → 도메인 → 테이블 → 화면 → ServiceId → DTO/Rule/SQL |
| 검증·생성 | 9~10 | 통합검증 → ZIP/코드·산출물 생성 |
| 구현·시험 | 11~15 | Git 적용 → 로직 구현 → 단위/통합 → 품질·보안 |
| 이관 | 16~19 | OM·산출물 → PR → 배포 → 변경/폐기 |

생성 성공 ≠ 개발 완료. **Compile · Test · Review · OM 등록 · 운영 확인**까지가 한 거래의 완성.

---

## 9. 용량·설정·운영 (I 경로 요약)

| 주제 | 핵심 |
|------|------|
| 용량 | TPS ↔ TPMC 환산, 동시사용자·코어·Heap·DB Pool 균형 |
| WAS | Tomcat thread · DeltaManager(세션) · JVM Heap vs OS 메모리 |
| DB | HikariCP 크기, Query Timeout과 Online Timeout 다층 |
| 프록시 | Apache / L4 Sticky / GSLB |
| 설정 문서 순 | Apache → Tomcat → Spring yml → Hikari → MyBatis → JVM → Logback → Filter → L4 → 체크리스트 |

운영 전 체크: `ztcfbook-m` 부록 H·I·J (개발 종료·리뷰·운영 전환).

---

## 10. 역할별 “이것만” 기억하기

### 신규 업무 개발자

1. 요청은 항상 `/online` + `serviceId`
2. Handler는 도메인 1개, Controller 금지
3. 6계층 책임 지키기
4. OM에 ServiceId·Timeout 등록
5. 가능하면 Model Studio로 골격 생성 후 로직만 보완

### 플랫폼 개발자

1. STF/ETF/거래통제는 공통 — 업무로 끌어내지 말 것
2. Gateway·JWT·EAI·Cache 경계 유지
3. 설계↔코드 Gap을 문서화(`zman/00`, `zarchitecture` README)

### 아키텍트·운영

1. E2E 경로·WAR 경계·DB 역할 분리
2. Timeout·풀·스레드를 용량 목표에 맞게
3. 배포는 bootRun과 ztomcat 이중 경로 인지

---

## 11. 3일 압축 학습 (목차 A 기준)

| 일차 | 내용 | 산출 |
|------|------|------|
| 1일 | TCF 정의 · 시스템 한 장 · serviceId | URL 하나 + serviceId 읽기 |
| 2일 | 6계층 · Handler · 명명 | Handler 스케치 |
| 3일 | 로컬 bootRun · SV 고객요약 실습 · 체크리스트 | curl 성공 + 부록 H |

이어서: 세션/JWT/OM → 담당 WAR `zguide` → AI 방법론(자동화 시).

---

## 12. 바로가기 (요약 → 원문)

| 보고 싶은 것 | 링크 |
|--------------|------|
| 전체 읽기 순서 | [NSIGHT-TCF-전체문서-읽기순서.md](./NSIGHT-TCF-전체문서-읽기순서.md) |
| 초보 입문 | [ztcfbook-m/00-목차.md](../../ztcfbook-m/00-목차.md) |
| 아키텍처 시작 | [zarchitecture/README.md](../../zarchitecture/README.md) |
| 설계서 요약 | [zman/README.md](../../zman/README.md) |
| 모듈 가이드 | [zguide/README.md](../../zguide/README.md) |
| TCF 개념 노트 | [zdocs-2/TCF.md](../../zdocs-2/TCF.md) |
| AI 개발 절차 | [ai-방법론.md](../2026-07-25-AI-Methology/ai-방법론.md) |

---

## 13. 한 문장 결론

> NSIGHT TCF는 **표준 전문과 공통 파이프라인(STF/ETF)** 으로 거래를 통제하고, 업무는 **도메인 Handler + 6계층**으로만 확장한다.  
> 문서는 입문(A)→구조(B/C)→실무(E)→자동화(H)→용량(I) 순으로 읽고, **설계서와 코드의 Handler 단위 차이**를 항상 염두에 둔다.
