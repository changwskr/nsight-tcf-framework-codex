# 배치·파일·캐시 TASK 상세 설명서

이 문서는 BFC 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [BFC-01](./BFC-01-온라인·배치-경계-detail.md) | 온라인·배치 경계 | P1 | AA |
| [BFC-02](./BFC-02-Batch-Job-구조-detail.md) | Batch Job 구조 | P1 | AA |
| [BFC-03](./BFC-03-Scheduler-detail.md) | Scheduler | P1 | TA |
| [BFC-04](./BFC-04-Batch-재시작-detail.md) | Batch 재시작 | P1 | AA |
| [BFC-05](./BFC-05-Batch-오류처리-detail.md) | Batch 오류처리 | P1 | AA |
| [BFC-06](./BFC-06-파일명-표준-detail.md) | 파일명 표준 | P1 | EAI |
| [BFC-07](./BFC-07-대용량-파일-detail.md) | 대용량 파일 | P1 | TA |
| [BFC-08](./BFC-08-파일-암호화-detail.md) | 파일 암호화 | P1 | SEC |
| [BFC-09](./BFC-09-파일-대사-detail.md) | 파일 대사 | P1 | EAI |
| [BFC-10](./BFC-10-Cache-대상-detail.md) | Cache 대상 | P1 | AA |
| [BFC-11](./BFC-11-Cache-Key-detail.md) | Cache Key | P1 | AA |
| [BFC-12](./BFC-12-Cache-TTL-detail.md) | Cache TTL | P1 | AA |
| [BFC-13](./BFC-13-Cache-무효화-detail.md) | Cache 무효화 | P1 | AA |
| [BFC-14](./BFC-14-Cache-장애-detail.md) | Cache 장애 | P2 | AA |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
