# P13 — Model Studio·개발 자동화 프롬프트

사용: `P0` + `P13` + 자동화 범위·승인 경계

```text
[P13 개발 자동화 도구 설계 작업]

NSIGHT-TCF 업무 개발을 자동화하는
Model Studio 또는 코드·산출물 생성기를 설계하라.

자동화 입력:

- 프로젝트
- 업무코드
- 도메인
- 화면
- 화면 이벤트
- ServiceId
- 거래코드
- Request·Response 필드
- Table·Column
- PK·조회조건
- 권한·감사
- Timeout
- 처리유형

자동 생성 대상:

- DTO
- Handler
- Facade
- Service
- Rule 골격
- DAO
- Mapper Interface
- Mapper XML
- SQL 초안
- 테스트 골격
- HTTP 요청 예시
- 화면설계서
- 거래설계서
- 프로그램설계서
- 추적성 매트릭스
- OM 등록정보
- DDL 초안

다음 항목은 자동 확정하지 말고 사람의 승인 대상으로 분리하라.

- 업무 규칙
- 데이터 소유권
- 트랜잭션 경계
- 다른 도메인 연동
- SQL 성능
- 인덱스
- 권한
- 개인정보
- 감사
- 예외 정책
- 운영 위험

다음 설계를 포함하라.

1. 메타모델
2. 객체 간 관계
3. 화면 Wizard
4. 상태 전이
5. 승인 Workflow
6. Template Version
7. Framework Version 호환성
8. 생성 소유영역과 수동 소유영역
9. 재생성·Merge 전략
10. 품질 Gate
11. Workspace 검증
12. Git·CI/CD 연계
13. OM 등록 연계
14. 변경 영향 분석
15. Drift 탐지

최종 결과는
'모델 → 코드 → 문서 → OM → 테스트 → 배포 → 운영로그'가
하나의 기준정보에서 파생되도록 설계하라.
```
