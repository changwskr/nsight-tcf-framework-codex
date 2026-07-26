# NSIGHT TCF — 읽기순서(A~J) 목차별 핵심 내용

생성일: 2026-07-25  
기준 목차: [NSIGHT-TCF-전체문서-읽기순서.md](./NSIGHT-TCF-전체문서-읽기순서.md)

**범위:** Markdown 문서만 (`.docx` / `.pdf` 제외)  
**목적:** 문서가 많아 통독이 어려울 때, **목차 구간별로 “무엇을 알아야 하는지”** 만 파악하기

---

## A. `ztcfbook-m` — 초보 입문 (핵심 내용)

### A-1. TCF가 무엇인가
- REST처럼 URL마다 API를 만들지 않는다.
- **주소는 하나:** `POST /{업무코드}/online`
- **기능 구분은** JSON `header.serviceId` (예: `SV.Customer.selectSummary`)
- TCF = 거래를 **받고·검사하고·실행하고·응답·기록**하는 공통 엔진  
  → 개발자는 **업무 로직**, TCF는 **Header·권한·Timeout·로그·오류 형식**

### A-2. 개발자가 처음 기억할 3가지
1. 요청·응답은 **표준 JSON 전문** (`header` + `body`)
2. 기능은 **serviceId**로 찾는다 (URL로 나누지 않음)
3. Handler는 **Facade만** 호출 (SQL·긴 분기 금지)

### A-3. 6계층 (역할만)
| 계층 | 하는 일 | 하지 말 것 |
|------|---------|-----------|
| Handler | serviceId 분기 → Facade | SQL, `@Transactional` |
| Facade | 거래 흐름·트랜잭션 | SQL 직접 |
| Service | 업무 순서 | HTTP·세션 |
| Rule | 검증·업무 규칙 | DB 연결 |
| DAO | Mapper 호출 | 업무 if 가득 |
| Mapper | SQL | — |

### A-4. 이름 규칙
- ServiceId: `{업무}.{도메인}.{행동}` → `SV.Customer.selectSummary`
- 거래코드: `{업무}-{유형}-{번호}` (통제·감사용)
- 행동 예: `selectList`, `selectOne`/`selectSummary`, `register`, `update`, `delete`

### A-5. 실습 축
- 로컬 bootRun → **SV 고객요약** 따라하기 → 목록·등록 한 걸음  
- 체크: 부록 H(개발 종료) · I(리뷰) · J(운영 전환)

---

## B. `zarchitecture` — 아키텍처 (핵심 내용)

### B-1. 전체 시스템
- E2E: Client/UI → GSLB·L4·Apache → Gateway(선택) → 업무 WAR·OM → **tcf-web → tcf-core** → 6계층 → DB
- 스택: Java 21 · Spring Boot 3.3 · Gradle · MyBatis · H2(Oracle MODE)
- NFR 목표(설계): 동시 36,000 · TPS 720 · P95 3초 · 가용성 99.99%

### B-2. TCF 엔진 모듈
| 모듈 | 역할 |
|------|------|
| tcf-util | 순수 유틸 (Spring 비의존) |
| tcf-core | STF / TCF / ETF, Dispatcher, Timeout, 거래통제 |
| tcf-web | `POST /online` HTTP 진입 |

파이프라인: `STF.preProcess` → TimeoutExecutor → Dispatcher(serviceId) → Handler → `ETF.postProcess`

### B-3. STF가 하는 일 (업무 전에)
Header 검증 · GUID/TraceId · 세션·인증·권한 · 거래통제 · Idempotency · Timeout 정책 · TX_START 로그

### B-4. 업무 WAR 9개
IC·PC·MS·SV·PD·EB·EP·SS·MG — 각각 독립 WAR, 동일 6계층, **businessCode + Context**로 구분  
예: SV `/sv` 8086, IC `/ic` 8082 … OM은 레거시 om-service → **tcf-om**

### B-5. 그 밖의 영역 한 줄
- OM: Catalog·거래통제·운영 UI  
- Gateway / JWT: 라우팅·토큰  
- EAI: WAR 간 호출 (Java 직접 참조 금지)  
- Cache / Batch / UI·uj / CICD: 기준정보·배치·화면·배포

---

## C. `zman` — 설계서 요약 (핵심 내용)

### C-1. 처리 구조 (5장)
> 모든 온라인 거래 = **`TCF.process()` 한 입구**  
> STF → Timeout → Dispatcher → Handler(→Facade…) → ETF

### C-2. Handler (8장)
- Handler = **TCF ↔ 업무 Adapter**
- ✅ serviceId 진입, Body→DTO, Facade 호출  
- ❌ Header·세션·통제, SQL, StandardResponse 조립
- **코드 최신:** 도메인당 Handler 1개 + `serviceIds()` + `switch`

### C-3. 설계 vs 코드 (00장 — 필수)
| 항목 | 설계서 | 코드 |
|------|--------|------|
| Handler 단위 | serviceId당 1클래스 | **도메인당 1개** |
| OM Handler | 다수(문서상 83 등) | **소수(~24)** |
| EAI | 원칙 | **tcf-eai 모듈** |

일치: StandardRequest/Response, `/online`, STF→Dispatcher→ETF, 6계층 패키지

### C-4. 장별 주제 지도 (기억용)
01~04 개요·범위·전체구조·모듈 → 05~08 TCF·전문·Dispatcher·Handler → 09~11 Gateway·세션·JWT → 12~15 OM·통제·Timeout·로그 → 16~18 Cache·Batch·파일 → 19~21 DB·Spring·CICD → 22 샘플 → 23~25 Gap·보완·마무리

---

## D. `zdocs-2` → `zdocs-1` — 개념·소스 (핵심 내용)

### D-1. `zdocs-2`에서 챙길 개념
- **TCF.md:** 프레임워크 vs 엔진 구분, STF/BTF/ETF 용어, Handler 중심·공통 파이프라인·업무 WAR 독립
- **어플리케이션계층.md:** 프레임워크 이후 업무 영역 = entry / application / persistence  
  목표: 진입점 통일 · 책임 분리 · 프레임워크 비침투
- **온라인·세션·인증·DAO·캐시·배치·Timeout·전문·예외·페이징·명명:** 주제별 “어떻게 동작하는지” 참조 노트
- **SV고객요약샘플:** 첫 거래 구현 참고

### D-2. `zdocs-1`에서 챙길 것
- **SOURCE_INDEX:** 소스 어디 있나
- **TCF_FRAMEWORK_GUIDE:** 처리 순서 + Handler `serviceIds()`/`switch` 표준 코드 형태
- **architecture/**: 계층·전문·거래·Timeout·세션·JWT·Gateway·관측성·연동계약 등 **상세 정의서**
- **manual/**: Gradle·환경변수·산출물·라이브러리 모듈 안내

---

## E. `zguide` (+ MD 입문서) — 개발 실무 (핵심 내용)

> Word 개발 매뉴얼(`znsight-guide-word`)은 **제외**. 동일 계열은 `zguide`·`znsight-man`(MD)으로 본다.

### E-1. 공통 개발 규칙
```text
POST /{업무코드}/online
  → TCF.process (tcf-core)
     → STF → Dispatcher(serviceId) → Handler
     → Facade → Service → Rule → DAO → Mapper
     → ETF → StandardResponse
```
- Controller 만들지 않음  
- Handler = 도메인당 1개  
- WAR 간 호출 = **tcf-eai만**  
- serviceId는 **OM Catalog + 거래통제**에 등록

### E-2. 모듈별 한 줄 (`zguide`)
| 구분 | 핵심 |
|------|------|
| tcf-core | 엔진(STF/ETF/Dispatcher). 업무 SQL 없음 |
| tcf-gateway / jwt | 라우팅·토큰 |
| tcf-om | 운영 Catalog·통제 (8097) |
| tcf-ui / uj | 화면 채널 |
| tcf-eai / cache / batch | 연동·캐시·배치 |
| sv/ic/eb/…-service | 담당 업무 WAR 가이드 |

### E-3. 패키지 표준 (`znsight-man` 13장 요지)
```text
com.nh.nsight.{업무코드}
  entry/handler, facade
  application/service, rule
  persistence/dao, mapper
  dto/request, response, …
```
패키지 = 계층·업무 경계·ServiceId 위치·Mapper 위치의 기준.

---

## F. `ztcf-집필본-md` / `ztcfbook` / `ztcfbook-h` — 심화 (핵심 내용)

> Word 집필본(`ztcf-집필본`)은 **제외**. MD·책 본문만.

### F-1. 집필본-md가 다루는 축
1. 학습·거래 여정 · 프로젝트 구조  
2. 화면/ServiceId에서 소스 찾기 · SQL·복잡도  
3. 첫 조회 완성 · DTO·Service/Repository · 등록 흐름  
4. 목록·페이징 · 등록 · 수정/삭제·상태전이 · 트랜잭션·배치  
5. 오류 표준 · 세션·권한·JWT · 내부/외부 연동  
6. 테스트 · 코드리뷰·Quality Gate · CI/CD · 로그·메트릭  
7. Cache · 파일·대용량·Batch · 성능·튜닝 · CRUD 심화 · 운영 전환

### F-2. ztcfbook / ztcfbook-h
- 같은 32장 체계를 **설계·운영 밀도**로 서술 (h = 아키텍트 보강)  
- A에서 익힌 개념의 **근거·예외·운영 표**를 여기서 채움

---

## G. `znsight-man` — 장 단위 표준 (핵심 내용)

Word가 아닌 **MD 입문서** 본문. 특히 자주 쓰는 핵심:

| 주제 | 핵심 |
|------|------|
| 패키지 구조 | 업무코드 → Base Package → 계층 Package → Class → ServiceId → Mapper |
| 명명 | `com.nh.nsight` 고정, 업무 소문자 패키지, PascalCase 클래스, camelCase 메서드 |
| Handler/Facade/Service/Rule/DAO | 계층 책임 = A·B와 동일, 더 세밀한 금지·허용 규칙 |
| ServiceId·거래코드·DTO·Validation·Header | 식별자·전문·검증 필드 표준 |
| 세션·JWT·Gateway·거래통제·Timeout | 공통 통제는 STF/OM, 업무 코드에 재구현 금지 |
| 테스트·CICD·OM 등록 | 거래 완성 조건에 포함 |

---

## H. AI·구축 방법론 — `ai-방법론.md` 등 (핵심 내용)

> 구축방법론 Word는 제외. **`ai-방법론.md`** 중심.

### H-1. 원칙
- **코드 먼저 쓰지 않는다.** 화면·거래·데이터·프로그램 구조를 **먼저 모델링**
- 자동화는 **골격·초안**, 개발자는 **업무 판단** (생성 성공 ≠ 개발 완료)
- 완료 = Compile · Test · Review · OM 등록 · 운영 확인

### H-2. 0~19 단계 (압축)
| 구간 | 단계 | 핵심 |
|------|------|------|
| 준비·모델링 | 0~8 | 요구사항→도메인→테이블→화면→ServiceId→DTO/Rule/SQL |
| 검증·생성 | 9~10 | 통합검증(오류 0) → ZIP·코드·문서 생성 |
| 구현·시험 | 11~15 | Git 병합 → 로직 보완 → 단위/통합 → 품질·보안 |
| 이관 | 16~19 | OM·산출물 → PR → 배포 → 변경/폐기 |

### H-3. 추적성
화면 이벤트 → ServiceId → Handler/계층 → SQL/테이블 → OM Timeout·감사

---

## I. 용량·설정 (Markdown만) — 핵심 내용

> `znsight-capacity-word`, `znsight-config-value-word` 등 **Word는 제외**.  
> MD: `ztcf-book-capacity-md`, (가능 시) `ztcf-engine-config-info`의 md/txt

### I-1. 용량산정 (`ztcf-book-capacity-md`)
- 사용자·세션 → **TPS** → **TPMC/CPU Core** → VM 대수·메모리 → **설정값으로 연결**
- 설정 계층: OS/VM → GSLB/L4 → WEB/Apache → Gateway → JVM → Tomcat → Spring/TCF → Hikari → MyBatis → 세션/JWT → Cache → Timeout 다층
- 표준 프로파일 예: 8C/32G, 16C/64~128G, 32C/256G
- 운영: 임계치·모니터링 · Timeout/자원고갈 흐름 · 전환 체크리스트 · 변경관리

### I-2. 기억할 공식 감각
- 동시사용자·TPS·DB Pool·Tomcat thread·Heap는 **따로 키우면 깨짐** → 한 세트로 맞춤
- Online Timeout과 Query Timeout은 **다층**으로 설계

---

## J. `ztcf-다이어리` — 핵심 내용

일자 메모이므로 “표준”보다 **결정·작업 맥락**:
- AI 방법론·Model Studio 진행
- 도메인/업무코드 정의 메모
- 용량(TPS/TPMC) 논의 등

→ 표준 학습은 A~I, 다이어리는 **왜 그렇게 했는지** 보조.

---

## 목차 순서 = 지식 쌓는 순서 (한눈에)

| 목차 | Markdown에서 얻는 핵심 |
|:---:|------------------------|
| **A** | URL 하나 + serviceId, 6계층, 이름, 첫 실습 |
| **B** | E2E 그림, TCF 모듈, WAR 맵, NFR |
| **C** | 설계 결론 + **Handler 단위가 코드와 다름** |
| **D** | 주제별 개념 심화 + 소스 인덱스 |
| **E** | 모듈별 개발 규칙·포트·패키지 |
| **F** | 조회→CRUD→연동→품질→운영 서사 |
| **G** | 장 단위 세부 표준(패키지·명명 등) |
| **H** | 모델 우선 0~19, 자동화 vs 사람 책임 |
| **I** | TPS→설정값 연결, Timeout·Pool 균형 |
| **J** | 일자별 결정 맥락 |

---

## 초압축 (10줄)

1. 모든 거래는 `POST /{업무}/online` + `serviceId`.  
2. TCF가 STF(검사·통제)·ETF(응답·로그)를 담당한다.  
3. 업무는 Handler→Facade→Service→Rule→DAO→Mapper만 작성한다.  
4. Handler는 **도메인당 1개** (`serviceIds` + switch).  
5. WAR끼리 Java로 부르지 말고 **tcf-eai**를 쓴다.  
6. ServiceId·Timeout은 **OM**에 등록한다.  
7. 설계서의 “serviceId당 Handler 1”은 **구 설계**, 코드는 도메인 Handler.  
8. 개발은 **모델→검증→생성→로직→테스트→OM→배포**.  
9. 용량은 TPS·Core·Thread·Pool·Heap을 **세트로** 맞춘다.  
10. Word는 제외하고, 위 핵심은 전부 **Markdown 경로(A~I)** 에 있다.

---

관련: [전체 읽기순서(링크 목차)](./NSIGHT-TCF-전체문서-읽기순서.md) · [README](./README.md)
