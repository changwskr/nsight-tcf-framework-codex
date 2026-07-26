# A16 — Architecture Gate

**선행:** [A15-RACI-거버넌스-변경.md](./A15-RACI-거버넌스-변경.md)  
**다음:** [A17-최종-아키텍처정의서.md](./A17-최종-아키텍처정의서.md)  
**결과 파일:** [`결과/A16-Architecture-Gate.md`](./결과/A16-Architecture-Gate.md)  
**템플릿:** [`…/templates/Architecture_Gate_템플릿.md`](./NSIGHT_TCF_Architecture_Definition_Interactive_Prompts/templates/Architecture_Gate_템플릿.md)  
**구 대응:** (신규 — CRUD C14와 동일 역할)

## 이 단계에서 얻는 것

최종 정의서 발행 전 Gate 판정

## 지금 이 질문을 붙여 넣으세요

```text
A-MASTER를 적용한다. 결과/A00~A15 와 원장을 모아 Gate를 판정하라.
최종 정의서(A17)는 통과/조건부 통과 전에 발행하지 마라. 질문 1개씩.
단계 끝이면 결과/A16-Architecture-Gate.md 와 원장 갱신 후
「다음 단계로 갈까요?」만 물어라.

A00~A15를 모아 Architecture Gate 판정안을 제시한 뒤 사용자 확인을 받아라.

판정: 통과 / 조건부 통과 / 보완 필요 / 중단
보완 필요면 가장 중요한 미확정 1개만 질문하라.

Gate 표 예: G1요구 G2구조 G3런타임 G4데이터·보안 G5운영·검증 G6거버넌스
```

## 추가 확인

SoT 구분 / 사람 유지 항목 / 금지패턴(업무 Controller·Mapper직호출·타WAR DAO) /
제외범위(배치·파일·Gateway신규) / 실클래스명 사용 /
포트·Context As-Is/To-Be / Open Issue Explicit

## Gate

- 판정문 존재
- 보완 시 A17 금지

## 결과 저장

`결과/A16-Architecture-Gate.md` + 원장.
