# 기술·인프라·용량 TASK 상세 설명서

이 문서는 INF 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [INF-01](./INF-01-물리-배포구조-detail.md) | 물리 배포구조 | P0 | TA |
| [INF-02](./INF-02-Tomcat-업무그룹-detail.md) | Tomcat 업무그룹 | P0 | TA |
| [INF-03](./INF-03-단일-Tomcat-다중-WAR-detail.md) | 단일 Tomcat 다중 WAR | P0 | TA |
| [INF-04](./INF-04-JVM-Heap-detail.md) | JVM Heap | P1 | TA |
| [INF-05](./INF-05-GC-정책-detail.md) | GC 정책 | P1 | TA |
| [INF-06](./INF-06-Tomcat-Thread-detail.md) | Tomcat Thread | P1 | TA |
| [INF-07](./INF-07-HikariCP-Pool-detail.md) | HikariCP Pool | P1 | TA |
| [INF-08](./INF-08-Port·Context-detail.md) | Port·Context | P0 | TA |
| [INF-09](./INF-09-공통-Library-detail.md) | 공통 Library | P0 | AA |
| [INF-10](./INF-10-환경설정-분리-detail.md) | 환경설정 분리 | P0 | DVO |
| [INF-11](./INF-11-Health-Check-detail.md) | Health Check | P1 | TA |
| [INF-12](./INF-12-L4·Apache-라우팅-detail.md) | L4·Apache 라우팅 | P0 | TA |
| [INF-13](./INF-13-무중단-배포-detail.md) | 무중단 배포 | P1 | DVO |
| [INF-14](./INF-14-Rollback-detail.md) | Rollback | P0 | DVO |
| [INF-15](./INF-15-장애-격리-detail.md) | 장애 격리 | P1 | TA |
| [INF-16](./INF-16-용량-Baseline-detail.md) | 용량 Baseline | P1 | TA |
| [INF-17](./INF-17-가용성-detail.md) | 가용성 | P1 | TA |
| [INF-18](./INF-18-DR-detail.md) | DR | P2 | TA |
| [INF-19](./INF-19-시간-동기화-detail.md) | 시간 동기화 | P1 | TA |
| [INF-20](./INF-20-인증서-관리-detail.md) | 인증서 관리 | P1 | TA |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
