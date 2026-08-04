# 데이터·DB·SQL TASK 상세 설명서

이 문서는 DATA 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [DATA-01](./DATA-01-데이터-소유권-detail.md) | 데이터 소유권 | P0 | DA |
| [DATA-02](./DATA-02-타-업무-DB-접근-detail.md) | 타 업무 DB 접근 | P0 | DA |
| [DATA-03](./DATA-03-논리-데이터-모델-detail.md) | 논리 데이터 모델 | P0 | DA |
| [DATA-04](./DATA-04-물리-데이터-모델-detail.md) | 물리 데이터 모델 | P0 | DA |
| [DATA-05](./DATA-05-표준용어·도메인-detail.md) | 표준용어·도메인 | P0 | DA |
| [DATA-06](./DATA-06-공통-컬럼-detail.md) | 공통 컬럼 | P0 | DA |
| [DATA-07](./DATA-07-논리삭제-detail.md) | 논리삭제 | P0 | DA |
| [DATA-08](./DATA-08-데이터-보존-detail.md) | 데이터 보존 | P1 | DA |
| [DATA-09](./DATA-09-조회-건수-제한-detail.md) | 조회 건수 제한 | P0 | DA |
| [DATA-10](./DATA-10-페이징-방식-detail.md) | 페이징 방식 | P0 | DA |
| [DATA-11](./DATA-11-SQL-작성표준-detail.md) | SQL 작성표준 | P0 | DBA |
| [DATA-12](./DATA-12-인덱스-설계-detail.md) | 인덱스 설계 | P1 | DBA |
| [DATA-13](./DATA-13-Lock-처리-detail.md) | Lock 처리 | P1 | DBA |
| [DATA-14](./DATA-14-동시성-제어-detail.md) | 동시성 제어 | P1 | DA |
| [DATA-15](./DATA-15-대량-데이터-detail.md) | 대량 데이터 | P1 | DA |
| [DATA-16](./DATA-16-DB-변경배포-detail.md) | DB 변경배포 | P1 | DBA |
| [DATA-17](./DATA-17-테스트-데이터-detail.md) | 테스트 데이터 | P0 | DA |
| [DATA-18](./DATA-18-DB-감사-detail.md) | DB 감사 | P1 | DA |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
