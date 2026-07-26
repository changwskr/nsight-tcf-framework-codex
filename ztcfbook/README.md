# ztcfbook — NSIGHT TCF Framework 완전 개발 가이드

TCF Framework **책** 원고 디렉터리입니다. 편(篇)별 디렉터리 · 장(章)별 Markdown 파일로 구성됩니다.

| 문서 | 설명 |
| --- | --- |
| **[00-목차.md](./00-목차.md)** | 책 전체 목차 (32장 + 14부록) |
| **_gen-book-chapters.cjs** | 장 파일 일괄 재생성 스크립트 |

## 디렉터리 구조

```text
ztcfbook/
├── 00-목차.md
├── README.md
├── 서문/
├── 제01편/ … 제10편/
└── 부록/
```

| 편 | 디렉터리 | 장 |
| --- | --- | --- |
| 서문 | [서문/](./서문/) | 0 |
| 제1편 | [제01편/](./제01편/) | 1~4 |
| 제2편 | [제02편/](./제02편/) | 5~7 |
| 제3편 | [제03편/](./제03편/) | 8~11 |
| 제4편 | [제04편/](./제04편/) | 12~14 |
| 제5편 | [제05편/](./제05편/) | 15~18 |
| 제6편 | [제06편/](./제06편/) | 19~20 |
| 제7편 | [제07편/](./제07편/) | 21 |
| 제8편 | [제08편/](./제08편/) | 22~23 |
| 제9편 | [제09편/](./제09편/) | 24~30 |
| 제10편 | [제10편/](./제10편/) | 31~32 |
| 부록 | [부록/](./부록/) | A~N |

## 장 파일 재생성

```bash
cd ztcfbook
node _gen-book-chapters.cjs
```

## 집필 상태

| 구분 | 상태 |
| --- | --- |
| **제1~10편 (제1~32장)** | 본문 집필 완료 — 절별 설명, 장 요약, 이전·다음 네비, **말미 출처 색인** |
| **서문 · 부록 A~N** | 본문 집필 완료 |
| **ztcfbook-m** | 초보자 입문서 — [README](../ztcfbook-m/README.md) |
| **ztcfbook-h** | **Master Edition** — [README](../ztcfbook-h/README.md) (아키텍트·코드 샘플) |

> `node _gen-book-chapters.cjs` 재실행 시 **기존 본문이 덮어씌워집니다.** 집필 완료 후에는 실행하지 마세요.

## 원본 자료

- [znsight-man](../znsight-man/README.md) · [docs/architecture](../zdocs-1/architecture/architecture.md)
- [zarchitecture](../zarchitecture/README.md) · [zguide](../zguide/README.md) · [zman](../zman/README.md)
