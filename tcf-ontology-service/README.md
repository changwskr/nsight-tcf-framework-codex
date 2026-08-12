# tcf-ontology-service

**tcf-ontology-service** = NSIGHT/PDMG의 업무·아키텍처·개발표준·프로그램·데이터 간 관계를  
기계가 이해할 수 있는 형태로 축적하고,  
신규 시스템 구축 시 기존 지식과 표준을 **조회·검증·추천**하는 서비스이다.

상세: [`docs/ontology-design/mission.md`](./docs/ontology-design/mission.md)  
정본: [`ontology/`](./ontology/) · ADR: [`docs/adr/ADR-0001-ontology-source-of-truth.md`](./docs/adr/ADR-0001-ontology-source-of-truth.md)

파일럿: PDMG (`pdmg-fw` / `pdmg-service` / `pdmg-ui`)

## 빠른 데모

```bat
RUN.bat
```

| URL | 내용 |
|-----|------|
| http://localhost:8098/health | 기동 확인 |
| http://localhost:8098/api/ontology/service/mgcoa9001S0 | 조회 (4축) |
| http://localhost:8098/api/ontology/impact?from=mgcoa9001 | 영향·추천 후보 |
| http://localhost:8098/api/ontology/path?system=MG&business=CO&function=A&program=mgcoa8888S0 | 계층 path 조회 |
| http://localhost:8098/api/ontology/recommend?intent=crud | 신규 구축 패턴 추천 |
| http://localhost:8098/api/ontology/meta-model | Meta Model |
| http://localhost:8098/api/ontology/relations | 관계 vocabulary |


## 주요 작업

```bat
scripts\build.bat                  rem clean test war (모듈 로컬 gradlew)
scripts\build.bat seedPdmg         rem 매핑 시드 초안
scripts\build.bat validatePdmg     rem 소스↔온톨로지 검증
scripts\import\seed-mappings.bat
scripts\export\prompt-context.bat mgcoa9001
```

| 문서 | 내용 |
|------|------|
| [`docs/operation/seed-mappings.md`](./docs/operation/seed-mappings.md) | 시드 자동화 |
| [`docs/api/impact-prompt.md`](./docs/api/impact-prompt.md) | 영향도·프롬프트 |
| [`docs/ontology-design/mapping-coverage-v0.2.md`](./docs/ontology-design/mapping-coverage-v0.2.md) | 매핑 커버리지 |

## Phase 상태

| Phase | 상태 |
|-------|------|
| 0 ADR + 개념사전 | 완료 |
| 1 시드 + 4축 조회 API | 완료 |
| 2 소스 스캔 import/validation | 완료 |
| 3 영향도·프롬프트 연동 | 완료 |
| 4 샘플 프로그램 매핑 확장 | 완료 |
| 5 시드 자동화 | 완료 |
| 6 Meta/Relations + path/recommend | 완료 |
