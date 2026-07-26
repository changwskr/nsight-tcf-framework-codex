---
id: cap-steps
title: 용량 산정 단계
section: oc
order: 21
screens: [/oc/capacity.html]
---

# 용량 산정 단계 (CAP)

[용량 산정 화면](/oc/capacity.html)에서 지점·사용자·시나리오를 입력하고 CAP-020~050 결과를 확인합니다.

## 화면 구성

| 단계 | 내용 |
|------|------|
| CAP-010 | 입력 조건 (지점, 사용자, TPMC/TPS 등) |
| CAP-020 | TPS 시나리오 |
| CAP-030 | AP·VM 대수 |
| CAP-040 | WAS Thread |
| CAP-050 | DB Pool |

## 입력 시 유의

- **실요청 사용자**와 **스트레스 TPS** 숫자가 같아도 의미가 다름 (5% 피크 vs 15% 스트레스)
- TPMC/TPS 비율은 VM 프로파일과 연동

## 다음 단계

산정 값은 [ENV-002](/oc/env-002.html) 「산정 실행」 시 기준정보·플래너에 반영됩니다.

## 심화 이론

- `ztcf-book-capacity-md/`
- `tcf-oc/README.md`
