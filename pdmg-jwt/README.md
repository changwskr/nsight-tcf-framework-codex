# pdmg-jwt

JWT 발급·관리 서비스. 패키지/네이밍과 전문(온라인 거래) 구조는 pdmg-service(mgcoa8888 등) 체계를 그대로 따른다.

- 대그룹: `mg` / 업무: `jw` / 세부: `a`
- 패키지: `nhnis.mg.jw.a`
- 프로그램: `mgjwa1000`(인증), `mgjwa1001`(토큰), `mgjwa1002`(로그인이력), `mgjwa1003`(Refresh), `mgjwa1004`(보안정책)

## 전문(온라인 거래) 구조

pdmg-service 와 동일하게 **타입드 DTO** 기반으로 계층을 구성한다.

```
Handler → Facade(@Transactional, ObjectMapper.convertValue) → [업무 선처리] → Service(타입드 DTOin/DTOout) → DAO(Map)
```

- **Handler** (`entry/handler/*`): `@ConditionalOnProperty(nhnis.fw.tcf.enabled=true)`. `serviceId` 로 분기해 Facade의 대응 메서드(`Object dtoBody`)만 호출한다.
  진입 시 `JwtClientContext.bind(context)` 로 현재 스레드에 거래 컨텍스트를 묶고, 종료 시 `finally` 에서 `JwtClientContext.clear()` 로 해제한다.
- **Facade** (`application/facade/*`): `@Transactional` 을 여기서 건다. `ObjectMapper.convertValue(dtoBody, XxxDTOin.class)` 로 타입 변환 후 Service 를 호출하고, Service 가 돌려준 타입드 `XxxDTOout` 을 그대로 반환한다.
- **Service** (`application/service/*`): `public XxxDTOout method(XxxDTOin input)` 형태의 공개 시그니처만 갖는다. DAO 호출에 필요한 파라미터는 내부에서 `Map<String,Object>` 로 구성해 DAO(Mybatis)에 넘긴다. 채널/클라이언트 IP/운영자 ID 는 인자 없는 `JwtClientContext.channelId()`/`clientIp()`/`userId()` 로 조회한다.
- **DTO** (`dto/*`): 순수 Jackson POJO(getter/setter) 로 작성한다. pdmg-service 의 `DataObject`/`FieldProperty` 같은 무거운 보일러플레이트는 쓰지 않는다 — UI(pdmg-ui) 가 참조하는 JSON 필드명을 그대로 유지하기 위해서다. 목록류(`*S0DTOout`) 는 `rows: List<Map<String,Object>>` 로 DAO 조회 결과 컬럼명을 그대로 노출한다.
- **Controller** (`application/controller/*`, TCF OFF 호환): `@ConditionalOnProperty(nhnis.fw.tcf.enabled=false, matchIfMissing=true)`. `serviceId` 별 `@PostMapping("/mgjwaXXXXyy")` 로 Service 를 직접 호출한다(Facade 를 거치지 않음. 별도 트랜잭션 경계가 필요하면 Service 메서드에 직접 건다).
- **BizPrePostAspect** (`entry/aspect/BizPrePostAspect`): pdmg-service `nhnis.mg.co.a.entry.aspect.BizPrePostAspect` 와 동일한 패턴으로, `nhnis.mg.jw.a.application.service..*` 를 pointcut 으로 업무 선/후처리 로그를 남긴다.

## 서비스 ID

| serviceId | 설명 |
|-----------|------|
| mgjwa1000C0 | 로그인 |
| mgjwa1000C1 | SSO 발급 |
| mgjwa1000U0 | Refresh |
| mgjwa1000D0 | Access 폐기 |
| mgjwa1000D1 | 로그아웃 |
| mgjwa1001S0 | 토큰 현황 |
| mgjwa1001D0 | 토큰 강제폐기 |
| mgjwa1002S0 | 로그인 이력 |
| mgjwa1003S0 | Refresh Token 조회 |
| mgjwa1004S0 | 보안정책 조회 |
| mgjwa1004U0 | 보안정책 수정 |

## 실행

```bat
script\run.bat
```

- Port: **8110**
- JWKS: `GET /.well-known/jwks.json`
- Online: `POST /online` (`hdr_nhnis` + `dto`)

계정: `admin01` / `nsight01!`
