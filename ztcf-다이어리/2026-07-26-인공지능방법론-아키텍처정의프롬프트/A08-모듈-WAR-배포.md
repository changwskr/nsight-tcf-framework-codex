# A08 — 모듈·WAR·배포

**선행:** [A07-런타임-거래흐름.md](./A07-런타임-거래흐름.md)  
**다음:** [A09-데이터-상태-연계.md](./A09-데이터-상태-연계.md)  
**결과 파일:** [`결과/A08-모듈-WAR-배포.md`](./결과/A08-모듈-WAR-배포.md)  
**구 대응:** M09  
**실소스:** `settings.gradle`, `build.gradle` businessModules, `tcf-ui/.../BusinessModuleDefinitions.java`, `av-service`/`ln-service` bootWar

## 이 단계에서 얻는 것

Gradle·WAR·Context·패키지·포트·Tomcat — **As-Is/To-Be 분리**

## 지금 이 질문을 붙여 넣으세요

```text
A-MASTER를 적용한다. settings.gradle·BusinessModuleDefinitions(tcf-ui)를 인용한다. 질문 1개씩.
단계 끝이면 결과/A08-모듈-WAR-배포.md 와 원장 갱신 후
「다음 단계로 갈까요?」만 물어라.

로컬 실행·배포 Baseline은?

1. bootRun 우선 + WAR(Tomcat) 동등 지원 (권장)
2. WAR/Tomcat만
3. 직접 입력

필수 정합 규칙(답과 함께 원장에 남겨라):
- 로컬 포트 SoT = tcf-ui BusinessModuleDefinitions (AV=8101, LN=8103 등).
  tcf-uj 목록과 다를 수 있음 → 충돌 시 Gap.
- bootRun application.yml context-path=/ 가 흔함.
  Tomcat /{biz} 는 배포 목표일 수 있음 → 혼동 금지.
- AV/LN은 settings에 있어도 zarchitecture 구 WAR맵·ztomcat에 없을 수 있음 → Drift.
```

## 추가 확인 (질문 1개씩)

모듈 목록 원칙 / WAR명(`av.war`/`ln.war`) / 장애 영역(VM·JVM·WAR)

## Gate

- 포트·모듈 등록 규칙이 충돌 검사 가능
- As-Is(소스)와 To-Be(문서/배포) 분리

## 결과 저장

`결과/A08-모듈-WAR-배포.md` + 원장.
