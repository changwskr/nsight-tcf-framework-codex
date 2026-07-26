# 부록 I. 코드 리뷰 체크리스트

| 항목 | 내용 |
| --- | --- |
| **부록** | I |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## I.1 목적

본 부록은 NSIGHT TCF Framework 기반 업무 개발 시 **Pull Request / Merge Request**, 선도개발 검증, 품질 Gate에서 사용할 코드 리뷰 체크리스트를 정의한다.

NSIGHT에서 코드 리뷰는 문법·스타일 검토가 아니라 **아키텍처 품질 통제 절차**이다.

```text
코드 리뷰
= 표준 6계층 구조 준수 확인
+ ServiceId / 거래코드 정합성 확인
+ 표준 전문 / 오류코드 / 로그 확인
+ SQL / 트랜잭션 / Timeout 확인
+ 보안 / 권한 / 마스킹 확인
+ 테스트 / 운영 등록 가능성 확인
```

운영 관점에서 가장 중요한 질문은 다음 네 가지이다.

- 로그만 보고 **어느 업무·거래**인지 알 수 있는가?
- ServiceId만 보고 **어느 Handler**가 실행되는지 알 수 있는가?
- 오류코드만 보고 **무엇을 조치**해야 하는지 알 수 있는가?
- SQL ID만 보고 **어느 Mapper·거래**에서 실행됐는지 알 수 있는가?

---

## I.2 리뷰 범위

| 리뷰 대상 | 주요 확인 내용 |
| --- | --- |
| **Java — Handler** | ServiceId 매핑, Body 변환, Facade 호출, 업무로직·SQL 금지 |
| **Java — Facade** | 유스케이스 조립, 트랜잭션 경계, `@Transactional(timeout)` |
| **Java — Service / Rule** | 업무 흐름·규칙 분리, Validation, 외부연계·재처리 판단 |
| **Java — DAO / Mapper Interface** | Mapper 호출만, DB 예외 표준 변환 |
| **DTO** | Request/Response 분리, Validation, 민감정보 미포함 |
| **Mapper XML** | SQL ID, Timeout, Paging, `#{}` 바인딩, Injection 방지 |
| **application.yml** | Profile 분리, Pool/Timeout, Secret 외부화 |
| **테스트 코드** | Rule/Service/Handler/Mapper/TCF 통합/오류/권한 |
| **운영 등록** | ServiceId, 거래코드, 거래통제, Timeout, 오류코드 준비 |
| **Gradle / CI** | bootWar, 빌드·테스트·정적분석 결과 |

리뷰어는 **변경 diff 전체**와 [부록 H](./H-개발-완료-체크리스트.md) 자체 점검 결과를 함께 확인한다.

---

## I.3 리뷰 절차

```text
[개발자]
  1. 개발 완료
  2. 부록 H 자체 체크리스트 수행
  3. 단위 테스트 / Gradle Build
        ↓
[Merge Request 생성]
        ↓
[동료 리뷰]
  1. 기능 구현·요구사항 충족
  2. 코드 품질·가독성
        ↓
[아키텍처 리뷰] (필요 시)
  1. 6계층 구조·명명 규칙
  2. 표준 전문·오류·로그
        ↓
[품질 리뷰]
  1. 테스트·커버리지 결과
  2. Checkstyle / PMD / SonarQube
  3. SQL 검증
        ↓
[승인 / 조건부 승인 / 보완 요청 / 반려]
```

| 리뷰 유형 | 담당 | 초점 |
| --- | --- | --- |
| 동료 리뷰 | 팀 개발자 | 기능·버그·테스트 |
| 아키텍처 리뷰 | 아키텍트 | 계층·ServiceId·표준 준수 |
| 품질 리뷰 | QA / DevOps | CI Gate, SQL, 커버리지 |
| 보안 리뷰 | 보안 (민감 거래) | 권한·마스킹·Injection |
| 운영 리뷰 | OM / 운영 (필요 시) | Catalog·거래통제 등록 |

---

## I.4 요약 체크리스트 (15항)

MR 승인 전 **15항 필수**를 빠르게 훑는다. 상세 점검은 I.5~I.12를 참조한다.

| No | 점검 영역 | 핵심 질문 | 필수 | 확인 |
| --- | --- | --- | --- | --- |
| 1 | 업무 식별체계 | 업무코드, Context, WAR, Package가 일치하는가? | Y | □ |
| 2 | ServiceId | `{업무코드}.{업무대상}.{처리행위}` 규칙을 지키는가? | Y | □ |
| 3 | 거래코드 | `{업무코드}-{거래유형}-{일련번호}` 규칙을 지키는가? | Y | □ |
| 4 | 6계층 구조 | Handler → Facade → Service → Rule → DAO/Mapper 흐름인가? | Y | □ |
| 5 | 표준 전문 | Request/Response가 header + body / result 구조인가? | Y | □ |
| 6 | DTO | Request, Response, 내부 DTO가 분리되어 있는가? | Y | □ |
| 7 | Validation | 필수값, 형식, 길이, 범위, 코드값 검증이 있는가? | Y | □ |
| 8 | SQL | Mapper XML, SQL ID, Paging, Timeout 기준을 지키는가? | Y | □ |
| 9 | 트랜잭션 | 변경 거래의 트랜잭션 경계가 Facade에 명확한가? | Y | □ |
| 10 | Timeout | 온라인, DB, 외부연계 Timeout이 반영되어 있는가? | Y | □ |
| 11 | 오류처리 | 표준 예외와 오류코드를 사용하는가? | Y | □ |
| 12 | 로그 | GUID, TraceId, ServiceId, 거래코드가 로그에 남는가? | Y | □ |
| 13 | 보안 | 권한, 마스킹, SQL Injection 방지가 반영되어 있는가? | Y | □ |
| 14 | 테스트 | 단위/통합/Mapper/오류 테스트가 있는가? | Y | □ |
| 15 | 운영 등록 | OM Catalog, 거래통제, 오류코드, Timeout 준비되었는가? | Y | □ |

---

## I.5 업무 식별체계 리뷰

| 점검 항목 | 확인 기준 | 확인 |
| --- | --- | --- |
| 업무코드 | 표준 9개 업무코드(ic, pc, ms, sv, pd, eb, ep, ss, mg 등) 사용 | □ |
| Context Path | 업무코드 소문자 (`/sv`, `/om`) | □ |
| WAR 파일명 | `{업무코드소문자}.war` | □ |
| Gradle Module | `{업무코드}-service` 또는 표준 모듈명 | □ |
| Package Root | 업무코드·도메인 기준 (`com.nh.nsight.marketing.sv`) | □ |
| Class Prefix | 업무코드 PascalCase (`SvCustomerService`) | □ |
| Mapper 위치 | `resources/mapper/{업무코드소문자}/` | □ |
| 오류코드 Prefix | `E-{DOMAIN}-{CATEGORY}-{NNNN}` (업무 또는 TCF/OM) | □ |

| 잘못된 예 | 문제 |
| --- | --- |
| `CustomerService` (Prefix 없음) | 업무 경계 불명확 |
| Context `/singleview` | 표준 `/sv` 불일치 |
| `singleview.war` | WAR 명명 표준 위반 |
| `mapper/common/CustomerMapper.xml` | 업무 Mapper 경계 위반 |
| 오류코드 `ERROR01` | 표준 Domain·Category 미준수 |

---

## I.6 ServiceId / 거래코드 리뷰

| 점검 항목 | 확인 기준 | 확인 |
| --- | --- | --- |
| ServiceId 형식 | `{업무코드}.{업무대상}.{처리행위}` | □ |
| ServiceId Prefix | Header `businessCode`와 일치 | □ |
| Handler 매핑 | `serviceIds()`에 Catalog SERVICE_ID 포함 | □ |
| Handler Bean 중복 | 동일 ServiceId 중복 선언 없음 | □ |
| 거래코드 형식 | `{업무코드}-{거래유형}-{일련번호}` | □ |
| 거래코드 Prefix | 업무코드와 일치 | □ |
| Catalog 매핑 | ServiceId ↔ transactionCode ↔ Handler 1세트 | □ |
| processingType | 거래유형(INQUIRY/REGISTER 등)과 코드·Catalog 일치 | □ |
| OM 등록 | `OM_SERVICE_CATALOG` 등록 대상 정리 | □ |
| 거래통제 | `TCF_TRANSACTION_CONTROL` 등록 대상 정리 | □ |

---

## I.7 6계층 구조 리뷰

| 계층 | 확인 기준 | 금지 사항 | 확인 |
| --- | --- | --- | --- |
| Handler | ServiceId 진입, Body 변환, Facade 호출 | DAO/Mapper 직접 호출 | □ |
| Facade | 유스케이스 조립, `@Transactional` | Rule 세부 구현, HTTP 응답 | □ |
| Service | 처리 절차, DAO/Adapter 조합 | HTTP 응답 직접 생성 | □ |
| Rule | Validation, 업무 조건 | DB 직접 접근 | □ |
| DAO | Mapper 호출, 예외 변환 | 업무 판단 | □ |
| Mapper | SQL 실행, Mapping | 사용자 메시지 생성 | □ |

**금지 패턴 (발견 시 반려 또는 보완 요청)**

| 금지 패턴 | 문제 |
| --- | --- |
| Controller/Handler에서 Mapper 직접 호출 | 계층 책임 위반 |
| Handler 내부 업무 로직·SQL | 진입점 책임 초과 |
| Rule에서 DB 직접 조회 | 판단·접근 혼합 |
| Java 코드 내 SQL 문자열 | SQL 추적 불가 |
| `catch (Exception e) {}` | 장애 은폐 |
| `RuntimeException("오류")` | 오류코드 추적 불가 |

---

## I.8 DTO / Validation 리뷰

| 점검 항목 | 확인 기준 | 확인 |
| --- | --- | --- |
| Request DTO | 전문 입력 구조와 매핑 | □ |
| Response DTO | 화면·전문 반환 데이터만 포함 | □ |
| 내부 DTO | DB Row·외부연계 DTO와 분리 | □ |
| Entity 노출 | DB Entity/Row를 응답에 직접 반환하지 않음 | □ |
| Bean Validation | `@NotBlank`, `@Size`, `@Pattern` 등 | □ |
| 업무 Validation | Rule 또는 Validator 추가 검증 | □ |
| 날짜·금액 | 형식 검증, 금액 `BigDecimal` | □ |
| 민감정보 | 응답 DTO에 불필요한 PII 없음 | □ |
| Map 남용 | Object/Map 남용 대신 명시적 DTO 권장(팀 규약) | □ |

---

## I.9 SQL / Mapper 리뷰

| 점검 항목 | 확인 기준 | 확인 |
| --- | --- | --- |
| Mapper XML 위치 | `resources/mapper/{업무코드}/` | □ |
| Namespace | Java Mapper FQCN 일치 | □ |
| SQL ID | Method명 일치, 추적용 주석 | □ |
| SELECT | `SELECT *` 금지 | □ |
| Paging | 목록조회 OFFSET/FETCH, pageSize 상한 | □ |
| Count SQL | 목록 Count 분리 | □ |
| ORDER BY | 고정 정렬 | □ |
| Timeout | RDW 3초, ADW 5초 등 | □ |
| `#{}` / `${}` | `#{}`만, 사용자 입력 `${}` 금지 | □ |
| 대량조회 | 온라인과 분리·Paging 필수 | □ |

---

## I.10 보안 / 로그 리뷰

| 점검 항목 | 확인 기준 | 확인 |
| --- | --- | --- |
| 인증 | 세션 또는 JWT 검증 | □ |
| 권한 | 메뉴·기능·데이터 권한 | □ |
| SQL Injection | `${}` 사용자 입력 금지 | □ |
| XSS | 출력 Escape·정제 | □ |
| 민감정보 | 로그·응답·예외에 평문 PII 없음 | □ |
| Token / Secret | 로그·Git·yml에 Secret 없음 | □ |
| GUID / TraceId | MDC·거래로그 포함 | □ |
| 거래로그 | PROCESSING → SUCCESS/FAIL/TIMEOUT | □ |
| 감사로그 | 조회·다운로드·권한위반·관리자 변경 | □ |
| 마스킹 | 고객번호·계좌·Token 마스킹 | □ |

보안 리뷰 원칙: 사용자 입력은 신뢰하지 않는다. Header 사용자정보는 세션 기준 재검증한다. 권한 실패는 감사로그 대상이다.

---

## I.11 테스트 리뷰

| 테스트 구분 | 확인 기준 | 확인 |
| --- | --- | --- |
| Rule | 업무 조건, 경계값 | □ |
| Service | DAO/Adapter 호출 흐름 | □ |
| Handler | serviceIds(), Facade 위임 | □ |
| TCF 통합 | `POST /{bc}/online` E2E | □ |
| Mapper | SQL ID, 파라미터, Mapping | □ |
| 오류 | 표준 오류코드·메시지 | □ |
| Timeout | TIMEOUT 상태·거래로그 | □ |
| 권한 | 미권한·타 지점 차단 | □ |
| CI | Compile·Unit Test 실패 0 | □ |
| 정적분석 | Blocker/Critical 0 (프로젝트 기준) | □ |

테스트 클래스명: `{대상Class}Test`. 메서드명은 시나리오가 드러나게 작성한다.

---

## I.12 운영 등록 리뷰

코드만 Merge하고 OM 미등록 상태로 운영 배포하면 **미등록 ServiceId 차단**으로 장애가 난다.

| 점검 항목 | 확인 기준 | 확인 |
| --- | --- | --- |
| Service Catalog | `OM_SERVICE_CATALOG` 등록·승인 | □ |
| 거래통제 | `TCF_TRANSACTION_CONTROL` 허용 조건 | □ |
| Timeout | `TCF_SERVICE_TIMEOUT_POLICY` 또는 Catalog | □ |
| 오류코드 | `OM_ERROR_CODE` 등록 | □ |
| 권한·메뉴 | 화면·기능 권한 매핑 | □ |
| Gateway | Route 등록(해당 시) | □ |
| Smoke Test | 대표 ServiceId 호출 시나리오 정의 | □ |

---

## I.13 판정 기준

| 판정 | 의미 | 처리 |
| --- | --- | --- |
| **승인** | 표준 위반 없음, Merge 가능 | Merge |
| **조건부 승인** | 명명·주석·정렬 등 경미한 보완, 운영 영향 낮음 | 보완 후 Merge |
| **보완 요청** | 표준 위반 또는 테스트 부족, 수정 후 재리뷰 | 수정 → 재리뷰 |
| **반려** | 구조 위반, 보안/SQL/거래통제 위반, 운영 장애 가능 | 재설계 또는 대폭 수정 |
| **보류** | 요구사항·설계 기준 불명확 | 기준 확정 후 재리뷰 |

**원칙**

- 보안·SQL Injection·거래통제 우회·Handler에서 DB 직접 접근 → **원칙적으로 반려**
- 테스트 없는 변경·핵심 시나리오 미검증 → **보완 요청**
- 주석·포맷·사소한 명명 → **조건부 승인** 가능

| 구분 | 승인 가능 | 승인 불가 |
| --- | --- | --- |
| 구조 | 6계층 책임 준수 | Handler/Controller에서 DB 직접 접근 |
| 명명 | ServiceId·거래코드 표준 | 임의 명명, Prefix 불일치 |
| SQL | Mapper XML, Paging, Timeout | Java SQL, `SELECT *`, `${}` |
| 오류 | BusinessException + 오류코드 | RuntimeException, 빈 catch |
| 운영 | OM 등록 대상 정리 | ServiceId/거래코드 미정의 |

---

## I.14 리뷰 결과 기록

| 항목 | 내용 |
| --- | --- |
| 리뷰 ID | CR-YYYYMMDD-0001 |
| 대상 Branch | |
| ServiceId | |
| 거래코드 | |
| 리뷰 유형 | 동료 / 아키텍처 / 품질 / 보안 / 운영 |
| 리뷰 결과 | 승인 / 조건부 승인 / 보완 요청 / 반려 |
| 주요 지적 | |
| 보완 담당·완료일 | |
| 재리뷰 여부 | Y / N |
| 최종 승인자 | |

**코멘트 작성:** "별로입니다"가 아니라 **기준 + 사유 + 수정 방향**을 적는다.  
예: "Handler에서 Mapper를 직접 호출하고 있어 계층 기준에 맞지 않습니다. Facade 또는 Service를 통해 호출하도록 수정해 주세요."

---

## 요약

코드 리뷰는 개발자 취향 검토가 아니라 **NSIGHT 운영 표준 준수 여부**를 검증하는 아키텍처 품질 Gate이다. 15항 요약 체크리스트로 MR을 빠르게 판단하고, I.5~I.12 상세 항목으로 식별체계·계층·DTO·SQL·보안·테스트·운영 등록을 점검한다. 승인·조건부 승인·보완·반려 판정은 운영 영향도와 표준 위반 심각도에 따라 일관되게 적용한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 H](./H-개발-완료-체크리스트.md) |
| → 다음 | [부록 J](./J-운영-전환-체크리스트.md) |

---

## 출처 색인

- [znsight-man/부록I-코드-리뷰-체크리스트.md](../../znsight-man/부록I-코드-리뷰-체크리스트.md)
- [znsight-man/61-코드-리뷰-기준.md](../../znsight-man/61-코드-리뷰-기준.md)
- [znsight-man/42-보안-코딩-기준.md](../../znsight-man/42-보안-코딩-기준.md)
- [znsight-man/12-애플리케이션-계층구조.md](../../znsight-man/12-애플리케이션-계층구조.md)
- [znsight-man/47-ServiceId-등록-절차.md](../../znsight-man/47-ServiceId-등록-절차.md)
- [znsight-man/62-품질-게이트-기준.md](../../znsight-man/62-품질-게이트-기준.md)
