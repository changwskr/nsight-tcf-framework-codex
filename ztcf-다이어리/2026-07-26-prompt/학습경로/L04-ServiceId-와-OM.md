# L04 — ServiceId와 OM (거래를 무엇으로 식별하는가)

**선행:** [L03](./L03-업무모듈-6계층.md)  
**다음:** [L05-DB-Mapper-SQL.md](./L05-DB-Mapper-SQL.md)  
**관련 프롬프트:** [`../P5-표준-산출물-추적성.md`](../P5-표준-산출물-추적성.md)

## 이 단계에서 얻는 것

`{업무}.{도메인}.{action}` 형태의 **ServiceId**가  
Handler 등록 · OM Catalog · Timeout · 로그를 잇는 **단일 키**임을 이해한다.

## 왜 이 순서인가

계층(L03)을 안 뒤에 “거래의 이름”이 운영정보와 어떻게 맞물리는지 본다.  
이 키가 없으면 추적성(L06)이 성립하지 않는다.

## 지금 이 질문을 붙여 넣으세요

```text
P0 + P5(추적성 관점)를 적용한다. 문서 양식 전체를 쓰지 말고, ServiceId 중심만 깊게 파라.

1. 현재 저장소에서 ServiceId 명명 규칙과 실제 예시 5개를 표로 모아라.
   (근거: Handler, sample-requests, OM 관련 시드/문서)
2. Handler의 serviceId()/serviceIds() 등록 → Dispatcher 매칭 → 실행까지의 연결을
   "식별자 관점"으로만 다시 그려라.
3. OM Service Catalog / Timeout / 거래통제가 ServiceId와 어떻게 연결되는지
   확인된 사실과 미확정을 구분해 보고하라.
4. tcf-ui의 BusinessModuleDefinitions 또는 sample-requests가
   ServiceId 테스트에 어떤 역할을 하는지 설명해라.
5. ServiceId가 바뀌거나 중복될 때 깨지는 것(화면, OM, 로그, 테스트)을 영향표로 작성하라.

산출물: 식별자 매트릭스 (ServiceId | Handler | 모듈 | 샘플요청 | OM언급여부 | 근거경로)
코드 생성 금지.
```

## 읽을 자료 / 열 파일

- 업무 Handler의 `serviceId` / `serviceIds`
- `tcf-ui` sample-requests, BusinessModuleDefinitions
- OM Catalog 관련 README/시드 (있으면)

## 자기 점검

1. ServiceId 없이 Handler만 있으면 운영에서 무엇이 불가능한가?  
2. 거래코드와 ServiceId는 같은가, 다른가?  
3. Timeout은 코드 상수인가, OM 정책인가? (확인된 사실만)  

## 통과 기준

- ServiceId 하나를 고르면 Handler·샘플·모듈을 연결해 말할 수 있다.  
