# 배치·파일·캐시 아키텍처 의사결정

이 디렉터리는 BFC 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [BFC-01](./BFC-01-온라인·배치-경계.md) | 온라인·배치 경계 | P1 | AA | SA |
| [BFC-02](./BFC-02-Batch-Job-구조.md) | Batch Job 구조 | P1 | AA | SA |
| [BFC-03](./BFC-03-Scheduler.md) | Scheduler | P1 | TA | SA |
| [BFC-04](./BFC-04-Batch-재시작.md) | Batch 재시작 | P1 | AA | SA |
| [BFC-05](./BFC-05-Batch-오류처리.md) | Batch 오류처리 | P1 | AA | SA |
| [BFC-06](./BFC-06-파일명-표준.md) | 파일명 표준 | P1 | EAI | SA |
| [BFC-07](./BFC-07-대용량-파일.md) | 대용량 파일 | P1 | TA | TA |
| [BFC-08](./BFC-08-파일-암호화.md) | 파일 암호화 | P1 | SEC | SEC |
| [BFC-09](./BFC-09-파일-대사.md) | 파일 대사 | P1 | EAI | SA |
| [BFC-10](./BFC-10-Cache-대상.md) | Cache 대상 | P1 | AA | SA |
| [BFC-11](./BFC-11-Cache-Key.md) | Cache Key | P1 | AA | AA |
| [BFC-12](./BFC-12-Cache-TTL.md) | Cache TTL | P1 | AA | SA |
| [BFC-13](./BFC-13-Cache-무효화.md) | Cache 무효화 | P1 | AA | SA |
| [BFC-14](./BFC-14-Cache-장애.md) | Cache 장애 | P2 | AA | SA |

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
