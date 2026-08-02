# Codex 오케스트레이터 템플릿

## 입력

- 사용자 요청
- 적용되는 `AGENTS.md`
- 역할 계약 경로
- 산출물 작업공간

## 실행

1. Analyst 계약을 읽고 분석 작업을 배정한다.
2. 분석 산출물의 존재, 상태와 완료 기준을 검사한다.
3. Builder에 승인된 분석과 소유 파일을 전달한다.
4. Builder 결과와 검증 명령을 QA에 전달한다.
5. QA PASS면 통합하고, FAIL이면 담당 역할에 제한된 수정 요청을 보낸다.

## 작업 메시지 형식

```text
역할: <role>
목표: <bounded objective>
입력: <paths and facts>
출력: <exact artifact paths>
완료 기준: <observable checks>
금지 범위: <files/actions outside scope>
```

## 종료

최종 보고에는 변경 파일, 실행한 명령, 결과, 미검증 범위와 사용자 소유 변경을 구분해 기록한다.
