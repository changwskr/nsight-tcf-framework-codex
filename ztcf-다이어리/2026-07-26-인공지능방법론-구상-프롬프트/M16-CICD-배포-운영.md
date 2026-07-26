# M16 — CI/CD·배포·운영 연결

**선행:** [M15-테스트-Quality-Gate.md](./M15-테스트-Quality-Gate.md)  
**다음:** [M17-선도개발-확산.md](./M17-선도개발-확산.md)  
**결과 파일:** [`결과/M16-CICD-배포-운영.md`](./결과/M16-CICD-배포-운영.md)

## 이 단계에서 얻는 것

AI 생성 결과를 **Git·CI/CD·OM·운영**까지 잇는 절차를 확정한다.

## 지금 이 질문을 붙여 넣으세요

```text
M00을 적용한다. 결과/M0~M15를 인용한다.
코드·최종방법론 본문은 쓰지 마라. 질문 1개씩.
파이프라인 YAML을 새로 만들지 말고, 절차·책임·증적으로 정의하라.
현재 저장소의 tcf-cicd / tcf-scripts / ztomcat 사실을 대조하라.

AI 생성 결과를 기존 Git·CI/CD 절차에 어떤 방식으로 반영할 예정입니까?

① 개발자가 검토 후 Commit
② 생성 전용 Branch와 Pull Request
③ Model Studio가 자동 PR 생성
④ 중앙 저장소에서 직접 배포
⑤ 아직 결정되지 않음

정의할 내용:
Workspace, 생성 Branch, Commit 규칙, PR, 자동검증, 리뷰·승인,
Artifact Version, 환경별 설정, DB Migration, OM 등록,
배포 순서, Health Check, Rollback, 운영 관찰기간

운영 연계:
Service Catalog, 거래통제, Timeout, 오류코드, 로그, 감사,
모니터링, Runtime 진단, 장애 Runbook, 변경·폐기

Rollback이 코드만으로 부족한 경우(DB·OM·Gateway Route)를 명시하라.

단계 산출물(결과/M16-CICD-배포-운영.md):
- Git 연계방안
- CI/CD Pipeline(개념)
- 배포·롤백 절차
- OM 연계방안
- 운영전환 체크리스트

확정 후 「다음 단계(M17)로 갈까요?」만 물어라.
```

## 단계 산출물

- Git 연계방안
- CI/CD Pipeline(개념)
- 배포·롤백 절차
- OM 연계방안
- 운영전환 체크리스트

## 통과 기준

- Commit/PR 방식이 확정되어 있다.  
- Rollback 범위에 DB·OM·Route가 포함되어 있다.  
