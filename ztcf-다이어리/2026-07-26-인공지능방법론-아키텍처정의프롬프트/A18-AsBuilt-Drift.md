# A18 — As-Built·Drift

**선행:** [A17-최종-아키텍처정의서.md](./A17-최종-아키텍처정의서.md)  
**다음:** 시리즈 종료 (필요 시 `재검증` 반복)  
**결과 파일:** [`결과/A18-AsBuilt-Drift.md`](./결과/A18-AsBuilt-Drift.md)  
**구 대응:** M18

## 이 단계에서 얻는 것

정의서 vs 소스·설정·OM·시험 Drift 표

## 지금 이 질문을 붙여 넣으세요

```text
A-MASTER를 적용한다. 결과/00-정의서(또는 A17)와 현재 Branch를 대조한다.
미실행은 성공으로 쓰지 마라. 질문 1개씩.
단계 끝이면 결과/A18-AsBuilt-Drift.md 와 원장 갱신 후
시리즈 종료를 선언하거나 「재검증을 반복할까요?」만 물어라.

Drift 검증 범위는?

1. 구조·명명·모듈·ServiceId·금지패턴 + 런타임 클래스명 정합 (권장)
2. 1 + OM·설정·샘플 POST 증적 + BusinessModuleDefinitions↔bootRun 포트
3. 전수 (ztomcat·zarchitecture 문서 Gap 포함) — 직접 지정

권장: 1 후 필요 시 2. AV/LN의 문서·배포 누락은 Drift 후보로 남긴다.
```

## 추가 확인

차이 목록 / 위험 / 시정·보류 / 완료 판정(문서≠완료)

## Gate

- Drift 표에 성공/실패/미실행
- 최종 판정문

## 결과 저장

`결과/A18-AsBuilt-Drift.md` + 원장.
