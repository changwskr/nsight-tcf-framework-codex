# TCF 모듈 재구성 내역

## 1. 변경 목적

기존 `common-core`, `common-web` 명칭은 범용 공통 모듈처럼 보이기 때문에, TCF Framework의 책임을 명확히 하기 위해 다음과 같이 변경했다.

| 변경 전 | 변경 후 | 목적 |
|---|---|---|
| `common-core` | `tcf-core` | TCF 표준 전문/Context/예외의 핵심 모듈화 |
| `common-web` | `tcf-web` | HTTP/JSON 온라인 거래 처리 엔진 모듈화 |
| 신규 | `tcf-util` | Core/Web 공통 Util 분리 |

## 2. 의존성 구조

```text
tcf-util
   ↑
tcf-core
   ↑
tcf-web
   ↑
업무 서비스(cc~om)
```

## 3. 패키지 변경

| 변경 전 패키지 | 변경 후 패키지 |
|---|---|
| `com.nh.nsight.common.core` | `com.nh.nsight.tcf.core` |
| `com.nh.nsight.common.web` | `com.nh.nsight.tcf.web` |
| `com.nh.nsight.common.core.util` | `com.nh.nsight.tcf.util` |

## 4. 업무 서비스 영향

업무 서비스는 기존 Handler/Facade/Service/Rule/DAO/Mapper 구조를 유지한다. 단, 표준 전문과 Handler Interface import는 다음으로 변경된다.

```java
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.web.transaction.TransactionHandler;
```
