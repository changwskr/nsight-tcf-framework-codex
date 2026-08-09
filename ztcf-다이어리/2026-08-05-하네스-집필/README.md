# 2026-08-05 하네스 집필

TCF/PDMP 관련 **하네스 패키지**를 한곳에 정리한 문서다.  
에이전트가 “어떤 하네스를 언제 쓰는가”를 빠르게 고르고, 실행·증적 계약을 잃지 않도록 하는 것이 목적이다.

## 문서 목록

| 문서 | 내용 |
|------|------|
| [01-하네스-지도.md](./01-하네스-지도.md) | 전체 지도, 레이어, 관계도 |
| [02-패키지-상세.md](./02-패키지-상세.md) | 패키지별 역할·산출물·검증 |
| [03-선택-가이드.md](./03-선택-가이드.md) | 상황별 선택 의사결정 |
| [04-PDMP-실행-워크플로.md](./04-PDMP-실행-워크플로.md) | Claude `/harness` + PDMP 실무 절차 |
| [05-실행-이력-mpcoa8888.md](./05-실행-이력-mpcoa8888.md) | 2026-08-05 안정화(옵션1) 실행 기록 |

## 한 줄 요약

| 패키지 | 한 줄 |
|--------|--------|
| `tcf-harness` | Java CLI 게이트 하네스 (요건→승인→구현→테스트) |
| `tcf-harness-prompt` | 위 하네스의 **프롬프트/템플릿만** 분리한 패키지 |
| `tcf-harness-framework` | Claude `/harness` **빈 템플릿** (프로젝트 이식용) |
| `tcf-harness-world` | Codex용 **메타 하네스** (역할·스킬 설계/검증) |
| `tcf-harness-pdmp` | PDMP **계약 문서 하네스** (대상 코드 수정 없음) |
| `tcf-harness-exe-pdmp` | PDMP **실행 하네스** (`../pdmp-service` + phases + execute.py) |
| `tcf-harness-exe-집필` | 책 **목차 구동 집필** (`TOC.md` + `chapters/{id}` → `../ztcfbook`) |

## 실무에서 지금 쓰는 것

- **PDMP 기능/안정화 구현**: `tcf-harness-exe-pdmp` + Claude `/harness`
- **책(ztcfbook) 집필·갱신**: `tcf-harness-exe-집필` + Claude `/harness`
- **계약만 점검·핸드오프 정의**: `tcf-harness-pdmp`
- **하네스 자체를 설계·검증**: `tcf-harness-world`
- **신규 Claude 프로젝트 시드**: `tcf-harness-framework`
- **게이트형 CLI 실험/장기 자동화**: `tcf-harness` (+ `tcf-harness-prompt`)

## 대상 코드베이스

- PDMP 실행 대상(기본): `pdmp-service`
- 책 집필 대상(기본): `ztcfbook` (변형: `ztcfbook-m`, `ztcfbook-h`)
- 프레임워크 분리 후보: `pdmp-fw` (아직 service 의존 미연결)
- 방법론·다이어리: `ztcf-다이어리`
