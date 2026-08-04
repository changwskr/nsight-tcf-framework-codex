## QA

Role: 검증자(QA)

Responsibilities:

- Builder가 생성한 산출물을 검증하고, 테스트 시나리오를 실행합니다.
- 주요 검증 결과를 `_workspace/{workId}/test-evidence/`에 저장합니다.

Inputs:

- implementation outputs

Outputs:

- test-evidence files, test-summary.md

Protocol:

- 실패 시 오케스트레이터에 리뷰 요청을 생성합니다.
