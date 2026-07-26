# A05 — 대안비교·ADR

**선행:** [A04-설계원칙-품질속성.md](./A04-설계원칙-품질속성.md)  
**다음:** [A06-목표-논리-애플리케이션.md](./A06-목표-논리-애플리케이션.md)  
**결과 파일:** [`결과/A05-대안비교-ADR.md`](./결과/A05-대안비교-ADR.md)  
**템플릿:** [`…/templates/ADR_템플릿.md`](./NSIGHT_TCF_Architecture_Definition_Interactive_Prompts/templates/ADR_템플릿.md)  
**구 대응:** M06

## 이 단계에서 얻는 것

주요 구조 대안 비교 + ADR 초안

## 지금 이 질문을 붙여 넣으세요

```text
A-MASTER를 적용한다. 원장·이전 결과를 인용한다. 질문 1개씩.
단계 끝이면 결과/A05-대안비교-ADR.md 와 원장 갱신 후
「다음 단계로 갈까요?」만 물어라.

첫 ADR로 다룰 결정은?

1. 업무 진입: 공통 OnlineTransactionController(/online) vs 업무 WAR Controller
2. 모듈 분할: WAR 단위 vs 모놀리스
3. 인증: Gateway JWT(있을 때) + 업무 WAR 직접접근 방어
4. 이번 범위에서 ADR 보류(후속) — Open Issue
5. 직접 입력

권장: 1 (실소스·방법론이 /online — ADR로 고정).
```

## 추가 확인 (질문 1개씩)

평가축(정합성·운영·보안·비용) / 결정·폐기 대안 / 승인 필요

## Gate

- 최소 1건 ADR 또는 명시적 보류

## 결과 저장

`결과/A05-대안비교-ADR.md` + 원장.
