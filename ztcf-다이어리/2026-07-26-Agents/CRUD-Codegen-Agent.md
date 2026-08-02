# CRUD Codegen Agent (다이어리)

> 정식 SoT: [xdoc/agents/crud-codegen-agent.md](../../xdoc/agents/crud-codegen-agent.md)

## 한 줄 정의

`결과*/`의 확정 마크다운을 읽어 TCF 6계층 업무 소스를 생성한다. C14 Gate 전이거나 원장 정합이 깨져 있으면 중단한다.

## 입력 계약

```text
결과폴더/
  _확정정보원장.md     ← SoT
  C14-설계-Gate.md     ← PASS | CONDITIONAL | FAIL
  C00 … C13 …          ← 상세 (원장과 충돌 시 사용자 확인)
```

필수 원장 키 예시:

| 키 | 예 |
| --- | --- |
| `c00.baseModule` / `c00.devMode` | `ln-service` / 신규·기존 |
| `c01.businessCode` · `c01.domainCode` | `AV` · `CustomerContact` |
| `c02`/`c06.serviceIds` | `AV.CustomerContact.selectList` … |
| `c04.tableName` · `c04.pk` | `LN_CUSTOMER_CONTACT` · `CONTACT_ID` |
| `c08.handler` · `c08.layout` | Handler명 · 6계층 |
| `c14.gate` | `PASS` / `CONDITIONAL` |

## Hard Gate

1. C14 = `FAIL` → 즉시 중단, 코드 금지  
2. C14 = `CONDITIONAL` → Open Issue 명시, 차단 이슈면 생성 전 확인  
3. BC / serviceId / Handler prefix / 대상 모듈 불일치 → 목록 확정 금지  
4. 파일 목록 미승인 → 소스 파일 쓰기 금지  

## 생성 순서 (C15)

```text
목록 승인
 → DTO/Criteria/Row
 → Rule → Mapper+XML → DAO
 → Service → Facade → Handler
 → 테스트 → 설정/schema
 → 샘플전문·Catalog/UI(범위 시)
 → C15 결과 MD · 원장 갱신
```

## 금지

- C14 전 코드 생성  
- 업무 Controller  
- Service → Mapper 직호출  
- 빈 성공 메서드  
- 보호(스킵) 파일 덮어쓰기  
- 미실행 테스트를 성공으로 보고  
- Framework 계약·Gateway 권한면제를 임의 변경  

## 기준 패턴

- C00 `baseModule`의 동등/유사 도메인을 복제한다.  
- 샘플: `ln-service`의 `LnCustomerContact*` → 대상 BC에 맞게 rename.  
- 이미 `av-service` 등에 구현이 있으면 **경로 기록·갭만 보완** (원장 `c15.impl`이 “이미 구현”인 경우).

## 완료 보고 형식

```markdown
## CRUD Codegen 결과
- 결과폴더:
- C14 Gate:
- 대상 모듈:
- serviceIds:
- 생성/수정/스킵 파일:
- 검증 명령·결과:
- Open Issue:
- 다음: C16
```

## 관련 프롬프트

- 실행: [prompts/C15-실행프롬프트.md](./prompts/C15-실행프롬프트.md)
- 방법론 C15: [../2026-07-26-인공지능방법론-CRUD개발프롬프트/C15-소스-설정-문서생성.md](../2026-07-26-인공지능방법론-CRUD개발프롬프트/C15-소스-설정-문서생성.md)
