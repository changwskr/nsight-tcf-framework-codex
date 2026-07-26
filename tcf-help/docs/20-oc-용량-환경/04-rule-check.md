---
id: rule-check
title: Rule 점검
section: oc
order: 23
screens: [/oc/rule-check.html]
---

# Rule 점검

[Rule 점검](/oc/rule-check.html)에서 설정 파일 업로드·Rule Engine·SC-007 대조를 수행합니다.

## 절차

1. **프로젝트 기준정보** — ENV-002 산정 반영 여부 확인
2. **설정 파일 업로드** — `application.yml`, Tomcat, MyBatis 등
3. **점검 실행** — THRESHOLD·RELATION 규칙
4. **SC-007 표** — 계층별 가이드 vs 현재값 (계층 그룹 표시)

## 필수 파일

| 파일 | 점검 |
|------|------|
| application.yml | 세션, Hikari, nsight.*, env-check |
| Tomcat (embedded) | threads, accept-count, connection-timeout |
| mybatis-config.xml | statementTimeout, fetchSize |

## SC-008 / SC-009

점검 실행 후 **Timeout Map**, **동시 요청 Flow** 카드가 표시됩니다.

## 종합 보고서 연동

Rule 실행 결과는 `/oc/check.html` 보고서에 요약 반영됩니다.

## 관련

- `tcf-oc/README.md`
- [ENV-002~004](?doc=env-flow)
