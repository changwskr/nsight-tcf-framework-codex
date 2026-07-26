# NSIGHT-TCF 아키텍처 정의 — 단계별 대화형 프롬프트

원본 안내(보완본): [`2026-07-26-인공지능방법론-아키텍처정의프롬프트.md`](./2026-07-26-인공지능방법론-아키텍처정의프롬프트.md)  
방법론 기준서: [`../2026-07-26-인공지능방법론-구상-프롬프트/결과/00-NSIGHT-TCF-AI-LLM-SI-개발방법론.md`](../2026-07-26-인공지능방법론-구상-프롬프트/결과/00-NSIGHT-TCF-AI-LLM-SI-개발방법론.md)  
CRUD 대화 방식 참고: [`../2026-07-26-인공지능방법론-CRUD개발프롬프트/`](../2026-07-26-인공지능방법론-CRUD개발프롬프트/)

한 번에 정의서를 쓰지 않는다.  
**A-MASTER 고정 → A00→A18 순차 질의 → 단계마다 `결과/`에 저장 → A16 Gate 통과 후에만 A17 최종 정의서 생성.**

## 프레임워크 정합 (필수)

상세: [`결과/_프레임워크정합점검.md`](./결과/_프레임워크정합점검.md)

| 항목 | 실소스 기준 |
| --- | --- |
| 온라인 | `OnlineTransactionController` → TCF → STF → **`OnlineTransactionTimeoutExecutor`** → Dispatcher → Handler → ETF |
| Controller | 공통 `/online` 허용 · **업무 WAR 거래 Controller 금지** |
| 계층 | 논리 6단 / 디스크 `entry`·`application`·`persistence` (av·ln) |
| 포트 | `tcf-ui` `BusinessModuleDefinitions` (AV 8101, LN 8103) |
| 시범 | `av-service`, `ln-service` + 방법론 CRUD 결과 |
| 문서 | zarchitecture·tcf-uj와 소스 불일치 시 **Gap** (단정 금지) |

## 사용 방식

```text
1. A-MASTER를 읽고 대화에 적용한다
2. A00부터 「지금 이 질문을 붙여 넣으세요」를 채팅에 붙인다
3. 답이 오면 확정표를 남기고, 단계 끝나면 결과/Axx-*.md 저장
4. 「현황」으로 원장을 본다 / 「다음」으로 다음 A로 이동한다
5. A16 Gate 통과 전「최종생성」을 하지 않는다
```

## 프롬프트 목록

| ID | 파일 | 결과 파일 |
| --- | --- | --- |
| A-MASTER | [A-MASTER-대화원칙.md](./A-MASTER-대화원칙.md) | (원칙만) |
| A00 | [A00-착수-기준원.md](./A00-착수-기준원.md) | [결과/A00-착수-기준원.md](./결과/A00-착수-기준원.md) |
| A01 | [A01-목적-범위-독자.md](./A01-목적-범위-독자.md) | [결과/A01-목적-범위-독자.md](./결과/A01-목적-범위-독자.md) |
| A02 | [A02-문제정의-현행구조.md](./A02-문제정의-현행구조.md) | [결과/A02-문제정의-현행구조.md](./결과/A02-문제정의-현행구조.md) |
| A03 | [A03-요구사항-제약.md](./A03-요구사항-제약.md) | [결과/A03-요구사항-제약.md](./결과/A03-요구사항-제약.md) |
| A04 | [A04-설계원칙-품질속성.md](./A04-설계원칙-품질속성.md) | [결과/A04-설계원칙-품질속성.md](./결과/A04-설계원칙-품질속성.md) |
| A05 | [A05-대안비교-ADR.md](./A05-대안비교-ADR.md) | [결과/A05-대안비교-ADR.md](./결과/A05-대안비교-ADR.md) |
| A06 | [A06-목표-논리-애플리케이션.md](./A06-목표-논리-애플리케이션.md) | [결과/A06-목표-논리-애플리케이션.md](./결과/A06-목표-논리-애플리케이션.md) |
| A07 | [A07-런타임-거래흐름.md](./A07-런타임-거래흐름.md) | [결과/A07-런타임-거래흐름.md](./결과/A07-런타임-거래흐름.md) |
| A08 | [A08-모듈-WAR-배포.md](./A08-모듈-WAR-배포.md) | [결과/A08-모듈-WAR-배포.md](./결과/A08-모듈-WAR-배포.md) |
| A09 | [A09-데이터-상태-연계.md](./A09-데이터-상태-연계.md) | [결과/A09-데이터-상태-연계.md](./결과/A09-데이터-상태-연계.md) |
| A10 | [A10-보안-개인정보-감사.md](./A10-보안-개인정보-감사.md) | [결과/A10-보안-개인정보-감사.md](./결과/A10-보안-개인정보-감사.md) |
| A11 | [A11-성능-용량-DR.md](./A11-성능-용량-DR.md) | [결과/A11-성능-용량-DR.md](./결과/A11-성능-용량-DR.md) |
| A12 | [A12-운영-모니터링-장애.md](./A12-운영-모니터링-장애.md) | [결과/A12-운영-모니터링-장애.md](./결과/A12-운영-모니터링-장애.md) |
| A13 | [A13-DevOps-자동검증-Gate.md](./A13-DevOps-자동검증-Gate.md) | [결과/A13-DevOps-자동검증-Gate.md](./결과/A13-DevOps-자동검증-Gate.md) |
| A14 | [A14-테스트-추적성.md](./A14-테스트-추적성.md) | [결과/A14-테스트-추적성.md](./결과/A14-테스트-추적성.md) |
| A15 | [A15-RACI-거버넌스-변경.md](./A15-RACI-거버넌스-변경.md) | [결과/A15-RACI-거버넌스-변경.md](./결과/A15-RACI-거버넌스-변경.md) |
| A16 | [A16-Architecture-Gate.md](./A16-Architecture-Gate.md) | [결과/A16-Architecture-Gate.md](./결과/A16-Architecture-Gate.md) |
| A17 | [A17-최종-아키텍처정의서.md](./A17-최종-아키텍처정의서.md) | [결과/A17-…](./결과/) · `결과/00-NSIGHT-TCF-아키텍처정의서.md` |
| A18 | [A18-AsBuilt-Drift.md](./A18-AsBuilt-Drift.md) | [결과/A18-AsBuilt-Drift.md](./결과/A18-AsBuilt-Drift.md) |

## 결과 저장

- 폴더: [`결과/`](./결과/README.md)
- 원장: [`결과/_확정정보원장.md`](./결과/_확정정보원장.md)
- 명령: `현황` / `수정: 항목=값` / `다음` / `최종생성`(A16 이후만)

## 구 패키지 (참고·보관)

[`NSIGHT_TCF_Architecture_Definition_Interactive_Prompts/`](./NSIGHT_TCF_Architecture_Definition_Interactive_Prompts/)  
초기 생성본(M00~M18·templates·state). **실행 기준은 본 README의 A-MASTER·A00~A18·`결과/`** 이다.  
템플릿은 구 패키지 `templates/` 를 재사용한다.

## 구→신 매핑

| 구 | 신 |
| --- | --- |
| M00 / MASTER | **A-MASTER** |
| M01~M16 | **A00~A15** |
| (신규) | **A16 Architecture Gate** |
| M17 | **A17** |
| M18 | **A18** |
| output/ | **결과/** (+ 단계별 Axx.md) |
