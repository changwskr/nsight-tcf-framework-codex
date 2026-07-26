# ztcfbook-h — NSIGHT TCF Master Edition

**아키텍트·시니어·플랫폼**용 TCF 완전 가이드. [`ztcfbook/`](../ztcfbook/) 전체 본문에 **아키텍처 다이어그램**, **Master 해설**(서술형), **코드베이스 샘플**, **Master Deep Dive**, **심화 참고**를 **47개 본문 장·부록 전체**에 통합했습니다.

## 문서 계층

| 문서 | 대상 | 특징 |
| --- | --- | --- |
| [ztcfbook-m](../ztcfbook-m/) | 초보 개발자 | 짧은 문장, 입문 |
| [ztcfbook](../ztcfbook/) | 업무·운영 개발자 | 32장 + 14부록, 출처 색인 |
| **ztcfbook-h** | 아키텍트·시니어 | **+ mermaid + 실제 소스 + ADR 링크** |

## 시작하기

1. [00-목차](./00-목차.md)
2. [서문](./서문/00-서문.md) — 문서 계층·Gap
3. [제3장 TCF 처리 엔진](./제01편/03-TCF-처리-엔진.md) — `TCF.process()` 샘플
4. [제22장 SV 실습](./제08편/22-조회-거래-SV-고객요약.md) — End-to-End 코드

## Master Edition 구성

각 장·부록:

| 블록 | 내용 |
| --- | --- |
| **아키텍처 뷰** | mermaid 시퀀스·플로우 (해당 장) |
| **Master 해설** | 아키텍트·시니어용 **서술형** 설명 (3~5문단) |
| **구현 샘플** | repo 실제 Java/XML/JSON 발췌 |
| **Master Deep Dive** | 아키텍트 관점 핵심·체크리스트 |
| **본문** | ztcfbook 동일 본문 |
| **심화 참고** | docs/architecture · zarchitecture · zman |

## 디렉터리

```text
ztcfbook-h/
├── 00-목차.md
├── _gen-book-h.cjs         ← ztcfbook 기반 재생성
├── _master-narrative.cjs   ← 47장 Master 해설(서술) 정의
├── _master-enrich-data.cjs ← mermaid·샘플·Deep Dive
├── 서문/
├── 제01편/ … 제10편/
└── 부록/ A~N
```

## 재생성

```bash
cd ztcfbook-h
node _gen-book-h.cjs
```

> ztcfbook 본문이 바뀌면 **재실행**하여 Master 블록을 다시 병합합니다.  
> `_master-narrative.cjs`에서 **Master 해설** 문단을, `_master-enrich-data.cjs`에서 다이어그램·샘플을 수정합니다.

## 역할별 경로 (Master)

| 역할 | 읽기 순서 |
| --- | --- |
| **아키텍트** | 서문 → 제1~4편 → 제31~32장 → 부록 K,L,M,N |
| **플랫폼** | 제3장 → 제15~16장 → 제24~27장 → docs/42,51 |
| **시니어 업무** | 제8~11장 → 제22~23장 → 부록 E,I,H |
| **DevOps/SRE** | 제19~20장 → 제21장 → 부록 J,G |

## 관련

- [docs/architecture](../zdocs-1/architecture/architecture.md)
- [zarchitecture](../zarchitecture/README.md)
- [zguide](../zguide/README.md)
