# C10 — 트랜잭션·오류·Timeout (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | C10 |
| 사용자 답변 | 롤백=2 조회만 / Timeout=1 5초 / 재시도·오류=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | 권한·시스템 오류코드 프레임 표준 |
| 설계 영향 | Facade timeout=5, OM Timeout=5, 자동 재시도 없음 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | C11 — 보안·개인정보·감사 |

---

## 트랜잭션

| 항목 | 내용 |
| --- | --- |
| CUD 롤백 | 해당 없음 |
| 조회 TX | Facade `@Transactional(readOnly=true, timeout=5)` |
| 위치 | Facade만 |

## Timeout·재시도·멱등

| 항목 | 내용 |
| --- | --- |
| Timeout | 5초 (Facade = OM) |
| 서버 재시도 | 없음 |
| 멱등 | selectList / selectDetail = 멱등 |

## 오류 분류

| 유형 | 코드/처리 |
| --- | --- |
| 입력 | LN-CCT-V001, V002 |
| 업무 | LN-CCT-E404 |
| 권한/DB/Timeout/시스템 | 공통 프레임 `[확인 필요]` |

## Gate

- [x] TX 위치 명확
- [x] 업무/시스템 오류 구분
- [x] Timeout·재시도 충돌 없음
