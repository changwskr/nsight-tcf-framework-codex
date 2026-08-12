# NSIGHT Development Context

## Requirement
Business=CO Function=A Transaction=QUERY Paging=YES

## Pattern
- name: ONLINE_PAGING_QUERY
- status: DERIVED_PATTERN
- derivedFrom: mgcoa5530, mgcoa8888

## Reference ServiceId
- mgcoa5530S0

## Architecture Structure
mgcoa5530S0 -> mgcoa5530Handler -> mgcoa5530Facade -> mgcoa5530Service -> mgcoa5530DAO -> mgcoa5530-ORA.xml -> TB_MK_CO_A_5530 -> [L5101, L5103]

## Message
- hdr_nhnis + dto

## Transaction
- TCF ON + Timeout (from candidate architecture)

## Paging
- 요구: YES (Ontology paging meta 없음 → DERIVED / UNRESOLVED detail)

## Rules
- overall: PASS, failCount: 0

## Evidence
- discoveredBy from structure steps (YamlGraphLoader)

## Unresolved
- Program ID (신규)
- ServiceId (신규)
- 신규 Table/Column

## Prohibited
- Ontology에 없는 JPA/Spring 임의 패턴

## Validation
- 구현 후 Architecture Gate 재실행

## Reference Prompt Context (Ontology)
# Ontology Prompt Context — mgcoa5530

> 이 블록은 `tcf-ontology-service`가 생성한 지식 허브 컨텍스트다.
> `32.범용CRUD프롬프트.md` 요구사항 작성·구현 시 **추정 금지**, 아래 계약을 우선한다.

## 프로그램

- programId: `mgcoa5530`
- title: 마케팅희망고객
- 분류: MG/CO/A (공통관리)
- packageRoot: `nhnis.mg.co.a`

## 아키텍처 계약

```text
RequestThread(TX밖): Filter → Interceptor.pre → Controller → TcfFacade → Future.get
Worker(TX안=TransactionTemplate): Dispatcher → Handler → Facade(REQUIRED) → BizPre → Service → DAO → BizPost → Deadline
RequestThread(TX밖): ResponseResolver → Interceptor.after → Filter clear
outcomes: {success=HTTP 200 + dto, bizException=ROLLBACK + result 오류전문, timeout=HTTP 504 FW_TIMEOUT then worker ROLLBACK, overload=HTTP 503 FW_OVERLOADED — TX not started}
```

## 컴포넌트 경계 규칙

- **R-HANDLER-NO-DAO** (error): Handler는 Facade 라우팅만. DAO/SQL/복잡 업무 금지
- **R-TX-OWNER-EXECUTOR** (error): 업무 DB TX 소유자는 TimeoutExecutor TransactionTemplate. Facade는 REQUIRED 참여
- **R-IMAGELOG-OUTSIDE-TX** (warning): ImageLog 요청/응답 기록은 업무 TX 밖. 업무 Rollback과 독립
- **R-BIZPREPOST-ON-SERVICE** (info): 업무 선후처리는 Service pointcut. 동일 rdw TX 참여
- **R-NAMING-SERVICEID-METHOD** (error): Facade/Service 메서드명 = 서비스 ID
- **R-PROGRAM-SINGLE-HANDLER** (error): 동일 식별번호(프로그램)는 Handler/Facade/Service/DAO 각각 하나

## 개발 매핑

- handler: `nhnis.mg.co.a.entry.handler.mgcoa5530Handler`
- facade: `nhnis.mg.co.a.application.facade.mgcoa5530Facade`
- controller: `nhnis.mg.co.a.application.controller.mgcoa5530Controller`
- service: `nhnis.mg.co.a.application.service.mgcoa5530Service`
- dao: `nhnis.mg.co.a.persistence.dao.mgcoa5530DAO`

## 서비스 ID

| serviceId | op | method | sqlIds |
|---|---|---|---|
| `mgcoa5530S0` | S | `mgcoa5530S0` | [mgcoa5530S0_S0, mgcoa5530S0_S0_count] |

## 데이터

- table: `TB_MK_CO_A_5530`
- pk: `[L5101, L5103]`
- mapperXml: `rdw.mg.co.a/mgcoa5530-ORA.xml`
- deleteMode: n/a

## 운영

- uiRoute: `/mgcoa5530/index.html`
- exceptionCodes: [MP0404]
- envelope success/error: {success={ hdr_nhnis, dto }, error={ hdr_nhnis, result }}
- samples: [sample-requests/mgcoa5530-list.json]

## 프롬프트 사용법

1. 위 컨텍스트를 CRUD 프롬프트 앞에 붙인다.
2. 미정 항목만 질문하고, 여기 있는 serviceId/table/FQCN은 재추정하지 않는다.
3. 구현 전 impact API로 변경 파일 목록을 확인한다: `GET /api/ontology/impact?from=mgcoa5530`

