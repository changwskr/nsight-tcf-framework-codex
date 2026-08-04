# 05 — 테스트·결함조치 단계 프롬프트

## 목표

사용자가 승인한 테스트 명령만 실행하고, 실패 시 원인을 분석하여 최대 3회 최소 수정·재시험한 뒤 증적을 남긴다.

## 입력

- 승인된 요건·분석·설계·구현 결과
- 승인된 테스트 명령
- 최근 실패 로그와 종료코드
- 현재 Git Diff

## 테스트 절차

1. 실행환경, Branch, Commit SHA를 기록한다.
2. 승인된 명령을 순서대로 실행한다.
3. 실패하면 로그와 변경사항을 근거로 직접 원인을 분류한다.
4. 승인 설계 안에서 최소 수정한다.
5. 같은 테스트를 다시 실행한다.
6. 최대 3회 이내 해결되지 않으면 `NEEDS_HUMAN_REVIEW`로 종료한다.
7. 모든 명령이 성공해도 사용자 승인 전 완료로 처리하지 않는다.

## 결함조치 금지사항

- 테스트 코드 삭제
- 테스트 비활성화 또는 조건 완화
- 오류를 숨기는 예외 처리
- 실패 명령을 승인 목록에서 제거
- 설계 변경을 우회한 임시 구현
- 작업 범위 밖 대규모 리팩터링

## 증적

```text
test-summary.md
environment.json
commands.json
changed-files.json
git-diff.patch
retry-history/attempt-NN/*-stdout.log
retry-history/attempt-NN/*-stderr.log
```

## 완료 판정

```text
PASS
= 모든 승인 명령 성공 + 증적 존재 + 금지행위 없음

NEEDS_HUMAN_REVIEW
= 3회 실패 또는 설계·보안·데이터 위험 발견
```
