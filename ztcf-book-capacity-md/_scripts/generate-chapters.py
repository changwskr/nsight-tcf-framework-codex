# -*- coding: utf-8 -*-
"""Generate ztcf-book-capacity-md chapter files from TOC."""
from pathlib import Path
import re

BOOK = Path(__file__).resolve().parent.parent
TOC = BOOK / "_tmp-toc" / "toc-lines.txt"
OUT = BOOK

COMMON = """## NSIGHT 1차 표준 전제

| 항목 | 기준값 |
|------|--------|
| 지점 수 | 3,600 |
| 지점당 사용자 | 6 |
| 전체 사용자 | 21,600 |
| 설계 세션 | 26,000~28,000 |
| 기본/피크/스트레스 TPS | 360 / 720 / 1,080 |
| 목표 응답 | p95 3초 이하 |
| 기준 VM | 8 vCPU / 32GB |
| VM당 TPS | 250 (보수) |
| AP 구조 | 2센터 Active-Active |

> 출처: `znsight-capacity-word` · 표준값은 성능시험으로 최종 보정
"""

SECTION_PLACEHOLDER = (
    "본 절은 `znsight-capacity-word` 원본 및 NSIGHT 1차 표준"
    "(3,600지점·21,600명·360/720/1,080 TPS·8CORE-32GB·250 TPS/VM)을 기준으로 작성합니다.\n\n"
)

CHAPTER_BODY = {
    1: COMMON + """
## 요약

용량산정은 **서버 대수만 계산하는 작업이 아닙니다.** 사용자 수 → 동시요청 → TPS → AP → DB Pool → DB Session → 장애 시 잔여 처리량이 하나의 체인으로 연결되어야 합니다.

### 핵심

- 정보계 AP는 RDW·마스킹·연계가 포함되어 단순 TPS 환산 위험
- 가정값·권고값·확정값 구분 (성능시험 후 확정)
- 제2부 산정 → 제3~4부 설정 → 제5부 프로파일 → 제7부 검증
""",
    7: COMMON + """
## 7.1 전체 변환 흐름

```
사용자·업무량 → 동시요청자 → TPS → 동시처리량 → CPU·VM 수량
  → Tomcat Thread → JVM Memory → DB Pool → Timeout
  → 네트워크 Connection → 운영 임계치
```

| 분류 | 예시 |
|------|------|
| 입력값 | 지점 수, 동시요청률, TPMC/TPS |
| 계산값 | TPS, AP 대수, WAS Thread, Pool |
| 설정값 | maxThreads, Xmx, maximumPoolSize |
| 임계치 | CPU 70%, Hikari Active 70% |
""",
    8: COMMON + """
## 8.1 지점 수와 사용자 수

```
전체 사용자 = 3,600 × 6 = 21,600
설계 세션 = 26,000 ~ 28,000 (20~30% 여유)
```

## 8.4 동시요청률

| 시나리오 | 비율 | 동시 요청자 |
|----------|------|-------------|
| 기본 | 5% | 1,080 |
| 피크 | 10% | 2,160 |
| 스트레스 | 15% | 3,240 |

**tcf-ui**: `/oc/capacity.html` CAP-010, ENV-002
""",
    9: COMMON + """
## TPS 산정

```
목표 TPS = 동시 요청자 ÷ 목표 응답시간(초)
```

| 시나리오 | TPS |
|----------|-----|
| 기본 | 360 |
| 피크 | 720 |
| 스트레스 | 1,080 |

p95 3초는 SLA. Thread 산정은 평균 1.0~1.2초 사용.
""",
    10: """
## TPMC 교차검증

| 업무 | TPMC/TPS |
|------|----------|
| Single View | 2,000~3,500 |
| 일반 정보계 | 1,200~2,000 |

**권장**: 3,000 TPMC/TPS, Core당 35 TPS → 8 Core 250 TPS/VM
""",
    11: """
## VM·서버 대수

| VM | vCPU | Memory | TPS/VM |
|----|------|--------|--------|
| 표준 | 8 | 32GB | 250 |

```
필요 AP = ⌈목표 TPS ÷ 250⌉
피크 720 TPS → 권장 8대 (센터당 4, N+1)
```

8CORE×8 vs 16CORE×4: VM 1대 장애 시 12.5% vs 25% → **온라인은 8CORE Scale-Out**
""",
    12: """
## 메모리 구성 (32GB VM)

| 구성 | 권장 |
|------|------|
| Heap (일반/SV) | 12GB / 14GB |
| Thread Stack | -Xss512k |
| Metaspace | Max 1GB |
| OS·Native·APM | 14~18GB 여유 |

세션·캐시에 대량 객체 저장 금지.
""",
    13: """
## Connection 산정

- Tomcat maxConnections: 10,000
- L4·Proxy KeepAlive: 120초 정합
- Hikari Pool: AP당 50 (별도 DB Connection)
- 센터 장애 시 잔여 Connection·대역폭 검증
""",
    16: """
## Timeout 계층

```
DB Query(3s) < Pool(3s) < Transaction(4~5s) < Proxy(10s) < WebTop(15s) < L4 Idle(120s)
```

10초 이상 동기 처리 금지. Timeout 후 무조건 재시도 금지.
""",
    22: """
## Tomcat 8CORE 권장

| 항목 | 값 |
|------|-----|
| maxThreads | 400~500 |
| acceptCount | 300~500 |
| maxConnections | 10,000 |

```
Thread = AP TPS × 1.2초 × 1.2 ≈ 360
```
""",
    23: """
## JVM 8CORE

- Xms=Xmx: 12GB (일반) / 14GB (SV)
- G1GC, MaxGCPauseMillis=200
- HeapDump, GC Log 필수
""",
    26: """
## HikariCP 산정

```
Pool = AP TPS × DB점유(0.15s) × 1.3 ≈ 50
상한 = WAS Thread × 30%
최종 = max(30, min(산출, 상한))
```

일반 50 / SingleView 60
""",
    37: """
## 8Core·32GB 프로파일

| 영역 | 값 |
|------|-----|
| TPS/VM | 250 |
| maxThreads | 400~500 |
| Heap | 12~14GB |
| Pool | 50/60 |
| Transaction | 4~5s |
""",
    45: """
## 정상 적용 예시 (피크 720 TPS)

1. 21,600명 × 10% = 2,160 동시요청
2. TPS = 2,160 ÷ 3 = 720
3. AP = ⌈720÷250⌉ = 3 → A-A 6대, 권장 8대
4. Thread ≈ 360, maxThreads 400~500
5. Pool = 50, 총 400 (8대×50)
6. 센터 장애 후 4대 → 1,000 TPS
""",
    46: """
## 금지 예시

- 사용자 수 = TPS로 직접 적용 ❌
- 세션 수 = Thread 수 ❌
- maxThreads = DB Pool 동일값 ❌
- 물리 메모리 50% = Heap 무조건 ❌
- 성능시험 없이 벤치마크 확정 ❌
""",
    47: """
## 성능시험 시나리오

| 시나리오 | TPS | 기준 |
|----------|-----|------|
| 기본 | 360 | p95≤3s |
| 피크 | 720 | Pool 고갈 없음 |
| 스트레스 | 1,080 | 한계 식별 |
| 센터/AP 장애 | — | 잔여 TPS |
""",
    49: """
## 운영 임계치

| 항목 | Warning | Critical |
|------|---------|----------|
| p95 | >3s | >5s |
| Heap | >70% | >85% |
| Busy Thread | >70% | >90% |
| Hikari Active | >70% | >90% |
""",
    57: """
## 마무리

- 용량산정 목적: **운영 가능한 안정성** 검증
- 설정값은 고정이 아닌 **Capacity Management** 대상
- 선도개발·성능시험·운영 실측으로 지속 보정
""",
}

APPENDIX_BODY = {
    "B": """# 부록 B. 사용자·세션·TPS 계산표

| 시나리오 | 동시요청률 | 동시요청자 | TPS |
|----------|------------|------------|-----|
| 기본 | 5% | 1,080 | 360 |
| 피크 | 10% | 2,160 | 720 |
| 스트레스 | 15% | 3,240 | 1,080 |
""",
    "C": """# 부록 C. TPMC·Core 교차검증표

| TPMC/TPS | Core당 TPS |
|----------|-------------|
| 2,000 | ~53 |
| 3,000 | ~35 |
| 4,000 | ~26 |
""",
    "F": """# 부록 F. 8C·16C·32C 표준 설정 프로파일

| VM | TPS/VM | maxThreads | Heap | Pool |
|----|--------|------------|------|------|
| 8C/32G | 250 | 400~500 | 12~14G | 50 |
| 16C/64G | 500 | 800~1000 | 24~28G | 80~100 |
| 32C/256G | 배치 | 별도 | 64G+ | 별도 |
""",
    "G": """# 부록 G. Tomcat Connector 설정 템플릿

```xml
<Connector port="8080"
  maxThreads="500" minSpareThreads="100"
  acceptCount="500" maxConnections="10000"
  connectionTimeout="8000" keepAliveTimeout="120000" />
```
""",
    "H": """# 부록 H. JVM 옵션 템플릿

```
-Xms12g -Xmx12g -Xss512k
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump
```
""",
    "K": """# 부록 K. HikariCP 설정 템플릿

```yaml
spring.datasource.hikari:
  maximum-pool-size: 50
  minimum-idle: 10
  connection-timeout: 3000
  max-lifetime: 1800000
  auto-commit: false
```
""",
    "M": """# 부록 M. Timeout 매트릭스

| 계층 | 초 |
|------|-----|
| MyBatis | 3 |
| Hikari | 3 |
| Transaction | 4~5 |
| Proxy | 10 |
| WebTopSuite | 15 |
| L4 Idle | 120 |
""",
    "V": """# 부록 V. 전체 DB Connection 합산표

```
DB Pool 총량 = AP 수 × AP당 Pool × DataSource 수
DB Session = Pool 총량 + 배치 + BI + ETL + 운영 + 여유
```
""",
    "W": """# 부록 W. 센터 장애 수용량표

| TPS | 권장 AP | 장애 후 AP | 잔여 TPS |
|-----|---------|------------|----------|
| 360 | 4 | 2/센터 | 500 |
| 720 | 8 | 4/센터 | 1,000 |
| 1080 | 12 | 6/센터 | 1,500 |
""",
    "Y": """# 부록 Y. 운영 임계치 표준표

| 항목 | Warning | Critical |
|------|---------|----------|
| p95 | >3s | >5s |
| Heap | >70% | >85% |
| GC p95 | >200ms | >500ms |
""",
}


def slugify(title: str) -> str:
    s = re.sub(r"^\d+\.\s*", "", title)
    s = re.sub(r"^\d+\.\d+\s*", "", s)
    s = re.sub(r"[·/\\:*?\"<>|]", "-", s)
    s = re.sub(r"\s+", "-", s.strip())
    s = re.sub(r"-+", "-", s)
    return s.strip("-")


def parse_toc(lines):
    part = None
    chapters = {}
    appendices = {}
    current = None
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if line.startswith("제") and "부." in line:
            part = line
            continue
        m = re.match(r"^(\d+)\.\s+(.+)$", line)
        if m and "." not in m.group(1) + ".":
            num = int(m.group(1))
            current = {"num": num, "title": m.group(2), "part": part, "sections": []}
            chapters[num] = current
            continue
        m = re.match(r"^(\d+)\.(\d+)\s+(.+)$", line)
        if m and current and int(m.group(1)) == current["num"]:
            current["sections"].append({"num": f"{m.group(1)}.{m.group(2)}", "title": m.group(3)})
            continue
        m = re.match(r"^([A-Z]{1,2})\.\s+(.+)$", line)
        if m:
            appendices[m.group(1)] = m.group(2)
    return chapters, appendices


def main():
    lines = TOC.read_text(encoding="utf-8").splitlines()
    chapters, appendices = parse_toc(lines)

    index = [
        "# NSIGHT 통합 용량산정 및 솔루션 환경설정 가이드 — 목차",
        "",
        "> 업무부하 산정에서 운영 설정·검증·변경관리까지",
        "> 원본: `znsight-capacity-word`",
        "",
        COMMON,
        "## 본문",
        "",
    ]
    last_part = None
    for num in sorted(chapters):
        ch = chapters[num]
        if ch["part"] and ch["part"] != last_part:
            index.append(f"### {ch['part']}")
            index.append("")
            last_part = ch["part"]
        fname = f"{num:02d}-{slugify(ch['title'])}.md"
        index.append(f"- [{num}. {ch['title']}](./{fname})")

        body = [f"# {num}. {ch['title']}", ""]
        if ch["part"]:
            body.append(f"> {ch['part']}")
            body.append("")
        body.append(CHAPTER_BODY.get(num, COMMON))
        body.append("")
        for sec in ch["sections"]:
            body.append(f"## {sec['num']} {sec['title']}")
            body.append("")
            body.append(SECTION_PLACEHOLDER)
        body.append("---")
        body.append("")
        body.append("[← 목차](./00-목차.md)")
        (OUT / fname).write_text("\n".join(body), encoding="utf-8")

    index.extend(["", "## 부록", ""])
    app_dir = OUT / "부록"
    app_dir.mkdir(exist_ok=True)
    for key in appendices:
        title = appendices[key]
        fname = f"{key}-{slugify(title)}.md"
        index.append(f"- [{key}. {title}](./부록/{fname})")
        if key in APPENDIX_BODY:
            content = APPENDIX_BODY[key]
        else:
            content = f"# 부록 {key}. {title}\n\n> `znsight-capacity-word` 참조\n\n표준 양식·템플릿은 운영 확정 시 채웁니다."
        content += "\n\n---\n\n[← 목차](../00-목차.md)\n"
        (app_dir / fname).write_text(content, encoding="utf-8")

    (OUT / "00-목차.md").write_text("\n".join(index) + "\n", encoding="utf-8")

    readme = f"""# NSIGHT 통합 용량산정 및 솔루션 환경설정 가이드

목차 docx 기준 **{len(chapters)}개 챕터 + {len(appendices)}개 부록** Markdown.

- [00-목차.md](./00-목차.md) — 전체 목차
- [zNSIGHT-용량산정-전체-흐름.md](./zNSIGHT-용량산정-전체-흐름.md) — 13단계 요약

원본: `znsight-capacity-word/`
"""
    (OUT / "README.md").write_text(readme, encoding="utf-8")
    print(f"OK: {len(chapters)} chapters, {len(appendices)} appendices")


if __name__ == "__main__":
    main()
