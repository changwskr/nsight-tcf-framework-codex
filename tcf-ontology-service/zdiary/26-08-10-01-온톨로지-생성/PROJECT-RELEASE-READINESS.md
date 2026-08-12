# PROJECT RELEASE READINESS

- 작성일: 2026-08-10
- 갱신: 2026-08-10 (26-08-10-15 잔여 P1 반영)
- 대상: `tcf-ontology-service`
- 준수표: `ONTOLOGY-15-COMPLIANCE-MATRIX.md`

---

# TCF ONTOLOGY SERVICE — RELEASE CANDIDATE / INTERNAL PILOT READY

Production Architecture Governance Ready(AuthN/RBAC, Application R-* Gate 실행기)는 아직 아니다.

## 증거

- **31 suites / 72 tests / 0 failures**
- productVersion `0.1.0-RC1`, knowledgeSnapshot `2026.08.10.03`
- P0 전부 DONE
- T-NEW-001~010 DONE
- Atomic reload + Evidence upgrade API PARTIAL+

## 운영

1. 로컬: `--spring.profiles.active=local` (RUN.bat)
2. 배포: admin mutation 기본 OFF
3. Design Gate `PASS_WITH_UNRESOLVED` ≠ 구현 완료
4. Gate family = ONTOLOGY_INTEGRITY_GATE (R-* Application Gate는 미실행)
