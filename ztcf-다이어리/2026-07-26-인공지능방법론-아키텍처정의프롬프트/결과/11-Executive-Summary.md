# Executive Summary — NSIGHT-TCF 아키텍처 정의

| 항목 | 내용 |
| --- | --- |
| 범위 | 온라인 거래 경로 |
| SoT | Branch `develop` |
| Gate | **조건부 통과** (A16) |
| 핵심 결정 | ADR-001: 공통 `/online` (OnlineTransactionController) |
| 계층 | entry/application/persistence · Handler→…→Mapper |
| 런타임 | TCF→STF→OnlineTransactionTimeoutExecutor→Dispatcher→Handler→ETF |
| 시범 | av-service (8101), ln-service (8103) |
| 승인자 | AA |
| 운영 반영 | AA 승인 전 불가 |

## 의사결정자에게

정의서는 **온라인 SI Baseline**으로 사용 가능.  
OM·NFR수치·배포맵·운영 보안은 Gap — 운영 전 AA 해소 필요.

본문: `00-NSIGHT-TCF-아키텍처정의서.md`
