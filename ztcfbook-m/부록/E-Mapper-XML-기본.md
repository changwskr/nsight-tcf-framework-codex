# 부록 E. Mapper XML 기본

| **부록** | E |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 부록 E](../ztcfbook/부록/E-Mapper-XML-템플릿.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  DAO[SvCustomerDao] --> XML[SvCustomerMapper.xml]
  XML --> SQL[RDW SELECT]
```

---

## SQL은 여기만

```text
Handler → Facade → Service → DAO → Mapper Interface → Mapper XML
```

**Java에 SQL 문자열 금지.** Rule·Handler에도 SQL 없음.

---

## 파일 위치

```text
src/main/resources/mapper/sv/SvCustomerMapper.xml
                         ↑업무코드 소문자
```

| | 규칙 |
| --- | --- |
| namespace | Java Mapper **FQCN**과 동일 |
| SQL `id` | Interface **메서드명**과 동일 |

---

## 조회 템플릿 (복사용)

```xml
<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

    <select id="selectCustomerSummary"
            parameterType="...CustomerSummaryCriteria"
            resultType="...CustomerSummaryRow"
            timeout="3">
        /* SQL_ID: SV.Customer.selectSummary */
        SELECT
              A.CUSTOMER_NO
            , A.CUSTOMER_NAME
        FROM RDW_CUSTOMER_SUMMARY A
        WHERE A.CUSTOMER_NO = #{customerNo}
    </select>

</mapper>
```

---

## 목록 + 페이징 (23장)

| SQL | 역할 |
| --- | --- |
| `selectXxxListPaging` | 목록 (OFFSET/FETCH) |
| `countXxxList` | **같은 WHERE**로 건수 |

| 항목 | 기준 |
| --- | --- |
| `SELECT *` | **금지** |
| Timeout | RDW **3초** (`timeout="3"`) |
| 바인딩 | **`#{}`만** — `${}` 금지 |
| ORDER BY | **필수** (목록) |

---

## ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| Handler에서 Mapper 호출 | **DAO만** |
| 페이징 없이 전체 SELECT | 대량 조회 **장애** |
| `${orderColumn}` 사용자 입력 | **SQL Injection** |
| RDW·ADW SQL 한 파일 | **Mapper 분리** |

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 D JSON](./D-JSON-예시.md) |
| → 다음 | [부록 F 오류코드](./F-오류코드-형식.md) |

---

## 📘 원본

- [ztcfbook/부록/E-Mapper-XML-템플릿.md](../ztcfbook/부록/E-Mapper-XML-템플릿.md)
- [sv-service/.../SvCustomerMapper.xml](../../sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml)
