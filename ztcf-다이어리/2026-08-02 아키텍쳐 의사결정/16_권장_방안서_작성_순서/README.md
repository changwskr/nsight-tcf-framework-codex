# 권장 방안서 작성 순서

상호 의존성과 변경 파급도를 고려해 상세 방안서를 작성·승인하는 권장 순서를 정의한다.

| ID | 항목 | 주관/참여 영역 |
|---|---|---|
| [PLAN-01](./PLAN-01-개인정보-마스킹-및-원문조회-통제-방안.md) | 개인정보 마스킹 및 원문조회 통제 방안 | DA·SEC·AA·UI |
| [PLAN-02](./PLAN-02-전문-필드-암호화-및-키관리-방안.md) | 전문 필드 암호화 및 키관리 방안 | SEC·MCA·EAI·AA |
| [PLAN-03](./PLAN-03-통합-명명·식별·추적성-방안.md) | 통합 명명·식별·추적성 방안 | SA·AA·UI·DA·FW |
| [PLAN-04](./PLAN-04-표준-전문-및-ServiceId-운영-방안.md) | 표준 전문 및 ServiceId 운영 방안 | MCA·FW·AA·OM |
| [PLAN-05](./PLAN-05-TCF-실행-흐름-및-계층-책임-방안.md) | TCF 실행 흐름 및 계층 책임 방안 | FW·AA |
| [PLAN-06](./PLAN-06-인증·JWT·권한·Header-정합성-방안.md) | 인증·JWT·권한·Header 정합성 방안 | SEC·AA·FW |
| [PLAN-07](./PLAN-07-오류·예외·표준-응답-방안.md) | 오류·예외·표준 응답 방안 | FW·AA·UI |
| [PLAN-08](./PLAN-08-트랜잭션·Timeout·멱등성-방안.md) | 트랜잭션·Timeout·멱등성 방안 | AA·TA·EAI·DA |
| [PLAN-09](./PLAN-09-MCA·EAI·업무-WAR-연계-방안.md) | MCA·EAI·업무 WAR 연계 방안 | SA·AA·EAI |
| [PLAN-10](./PLAN-10-데이터-소유권-및-DB-접근-통제-방안.md) | 데이터 소유권 및 DB 접근 통제 방안 | DA·AA·DBA |
| [PLAN-11](./PLAN-11-로그·감사·운영-추적-방안.md) | 로그·감사·운영 추적 방안 | FW·SEC·OPS |
| [PLAN-12](./PLAN-12-Tomcat·WAR·JVM·DB-Pool-배치-방안.md) | Tomcat·WAR·JVM·DB Pool 배치 방안 | TA·AA·DA |
| [PLAN-13](./PLAN-13-파일·엑셀·배치·대용량-처리-방안.md) | 파일·엑셀·배치·대용량 처리 방안 | AA·DA·TA |
| [PLAN-14](./PLAN-14-CI-CD·Architecture-Quality-Gate-방안.md) | CI/CD·Architecture Quality Gate 방안 | DVO·QA·AA |
| [PLAN-15](./PLAN-15-운영진단·장애대응·Runbook-방안.md) | 운영진단·장애대응·Runbook 방안 | OPS·TA·AA·DA |

## 공통 원칙

- 개별 문서는 요약·진입점이며 최종 결정은 관련 TASK와 ADR에서 관리한다.
- 문서 승인, 구현, 테스트, 자동검증과 운영 증적이 모두 연결되어야 완료다.
