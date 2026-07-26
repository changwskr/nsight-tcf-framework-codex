# A08 — 모듈·WAR·배포 (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | A08 |
| 사용자 답변 | 실행=1 bootRun+WAR / 등록규칙=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | Tomcat context To-Be · ztomcat 전수 |
| 설계 영향 | A18 Drift · 신규 모듈 체크리스트 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | A09 — 데이터·상태·연계 |

---

## 실행·배포

| 항목 | 값 |
| --- | --- |
| Baseline | bootRun 우선 + WAR(Tomcat) 동등 |
| 포트 SoT | tcf-ui BusinessModuleDefinitions |
| 시범 | AV=8101, LN=8103 |
| Context As-Is | bootRun `/` |
| Context To-Be | Tomcat `/{biz}` 가능 |

## 등록·명명

| 항목 | 규칙 |
| --- | --- |
| Gradle | settings include + businessModules |
| WAR | `{biz}.war` |
| Gap | zarchitecture/ztomcat 미등재 가능 |

## Gate

- [x] 포트·모듈 충돌 검사 가능
- [x] As-Is/To-Be 분리
