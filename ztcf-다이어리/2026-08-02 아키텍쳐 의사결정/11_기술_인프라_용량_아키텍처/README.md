# 기술·인프라·용량 아키텍처 의사결정

이 디렉터리는 INF 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [INF-01](./INF-01-물리-배포구조.md) | 물리 배포구조 | P0 | TA | SA |
| [INF-02](./INF-02-Tomcat-업무그룹.md) | Tomcat 업무그룹 | P0 | TA | SA |
| [INF-03](./INF-03-단일-Tomcat-다중-WAR.md) | 단일 Tomcat 다중 WAR | P0 | TA | SA |
| [INF-04](./INF-04-JVM-Heap.md) | JVM Heap | P1 | TA | TA |
| [INF-05](./INF-05-GC-정책.md) | GC 정책 | P1 | TA | TA |
| [INF-06](./INF-06-Tomcat-Thread.md) | Tomcat Thread | P1 | TA | TA |
| [INF-07](./INF-07-HikariCP-Pool.md) | HikariCP Pool | P1 | TA | TA·DA |
| [INF-08](./INF-08-Port·Context.md) | Port·Context | P0 | TA | AA |
| [INF-09](./INF-09-공통-Library.md) | 공통 Library | P0 | AA | SA |
| [INF-10](./INF-10-환경설정-분리.md) | 환경설정 분리 | P0 | DVO | TA |
| [INF-11](./INF-11-Health-Check.md) | Health Check | P1 | TA | TA |
| [INF-12](./INF-12-L4·Apache-라우팅.md) | L4·Apache 라우팅 | P0 | TA | SA |
| [INF-13](./INF-13-무중단-배포.md) | 무중단 배포 | P1 | DVO | TA |
| [INF-14](./INF-14-Rollback.md) | Rollback | P0 | DVO | SA |
| [INF-15](./INF-15-장애-격리.md) | 장애 격리 | P1 | TA | SA |
| [INF-16](./INF-16-용량-Baseline.md) | 용량 Baseline | P1 | TA | SA |
| [INF-17](./INF-17-가용성.md) | 가용성 | P1 | TA | SA |
| [INF-18](./INF-18-DR.md) | DR | P2 | TA | SA |
| [INF-19](./INF-19-시간-동기화.md) | 시간 동기화 | P1 | TA | TA |
| [INF-20](./INF-20-인증서-관리.md) | 인증서 관리 | P1 | TA | SEC |

## 공통 완료 기준

```text
ADR 승인
+ 개발표준 반영
+ 공통 샘플 또는 모듈 제공
+ 테스트 기준 및 결과
+ CI/CD 자동검증
+ 업무팀·운영 적용 확인
= TASK 완료
```

## 기준 문서

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
