# 거래설계서 — SV.Customer.selectSummary

## 거래 식별

| 항목 | 값 |
|---|---|
| 업무코드 | `SV` |
| 도메인 | `Customer` |
| ServiceId | `SV.Customer.selectSummary` |
| 거래코드 | `SV-INQ-0001` |
| 처리유형 | `SELECT_ONE` |
| Timeout | `3초` |
| 감사대상 | `Y` |
| 권한코드 | `SV_CUSTOMER_INQUIRY` |

## 실행 프로그램

| 계층 | 클래스/메서드 |
|---|---|
| Controller | `OnlineTransactionController.online()` |
| Handler | `com.nh.nsight.marketing.sv.entry.handler.SvCustomerHandler` |
| Facade | `com.nh.nsight.marketing.sv.entry.facade.SvCustomerFacade.selectCustomerSummary()` |
| Service | `com.nh.nsight.marketing.sv.application.service.SvCustomerService.selectCustomerSummary()` |
| Rule | `com.nh.nsight.marketing.sv.application.rule.SvCustomerRule` |
| DAO | `com.nh.nsight.marketing.sv.persistence.dao.SvCustomerDao.selectCustomerSummary()` |
| Mapper | `com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper.selectCustomerSummary` |
| Table | `SV_CUSTOMER` |

## 정상 흐름

```text
화면 이벤트
→ ServiceId 포함 표준전문
→ STF: Header·인증·권한·거래통제·Timeout 검증
→ Handler: ServiceId 분기
→ Facade: Transaction 경계
→ Service: 유스케이스 조립
→ Rule: 필수값·업무규칙 검증
→ DAO/Mapper: SQL 실행
→ ETF: 표준 성공 응답 및 거래로그 종료
```

## 오류·Timeout 흐름

- 필수값·업무규칙 위반: `BusinessException`으로 업무 오류 표준화
- Mapper/DB 오류: 시스템 오류로 변환하고 Rollback
- Timeout: TCF 전체 Timeout과 MyBatis Statement Timeout을 모두 적용
- 미등록 ServiceId: Dispatcher/Handler에서 실행 차단
- 변경 거래 감사로그: 사용자·지점·화면·ServiceId·변경대상·결과를 기록
