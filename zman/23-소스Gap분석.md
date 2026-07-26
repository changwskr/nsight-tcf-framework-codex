# 23장. 소스 Gap 분석 — 설명

## 설계서 절 목차

23.1~23.2 개요 · 23.3 강점 · 23.4 목표 · 23.5 영역별 Gap · 23.6~23.7 우선순위·요약 · 23.8~23.9 마무리·시사점

---

## 핵심 결론

| | |
|---|---|
| **현재** | TCF 골격·표준전문·Dispatcher·OM·Gateway·JWT·Cache **보유** |
| **운영** | 보안·기준정보 운영·CI/CD·17 WAR·품질 Gate **보강** |

---

## 현재 강점 (23.3)

- Gradle 멀티 모듈 (tcf-* + 9 WAR)  
- STF/Dispatcher/ETF  
- **도메인 Handler** (tcf-om 24개)  
- **tcf-eai** IC↔SV  
- tcf-gateway, jwt, batch, cache, ztomcat  
- docs / zdoc / **zman**  

## Gap 축소 (최근)

| 항목 | 이전 | 현재 |
|------|------|------|
| Handler | serviceId당 1 | **도메인당 1**, serviceIds() |
| OM Handler | 83 | **24** |
| EAI | 설계 | **tcf-eai** |
| 문서 | docx | **zman+docs** |

## 목표 운영 (23.4)

36K user / 43K session / TPS 720 / P95 3s / 99.99%  
Apache, Tomcat, Session JDBC, RDW/ADW, GitLab CI/CD, **17 WAR**

## 영역별 Gap (23.5)

| 영역 | Gap |
|------|-----|
| 업무 WAR | 9 → 17 |
| TCF Core | Idempotency, 마스킹, 감사정책 운영화 |
| OM | UI·프로세스 실운영 검증 |
| Gateway | Route DB 운영 |
| JWT/SSO | IdP 연동, Token 정책 |
| DB/MyBatis | RDW/ADW 분리, SQL 표준 |
| Spring | prd Profile, Secret |
| CI/CD | SonarQube, Nexus, Pipeline |

## Gap 요약 (23.8)

1. WAR 확장  
2. OM Catalog·TC·Timeout 운영  
3. 세션·JWT·SSO 강화  
4. 로그·Gateway·Batch 조회  
5. CI/CD·Rollback  
6. MyBatis SQL 표준  

## 시사점 (23.9)

골격 ✅ → **운영 가능** = P1(24장) 완료

## 이전 · 다음

← [22장 샘플](./22-업무서비스샘플.md) · [24장 보완](./24-보완과제-우선순위.md) →
