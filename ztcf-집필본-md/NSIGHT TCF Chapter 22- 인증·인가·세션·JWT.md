<!-- source: ztcf-집필본/NSIGHT TCF Chapter 22- 인증·인가·세션·JWT.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제22장. 인증·인가·세션·JWT

## 도입 전 안내말

제21장에서는 입력 오류·업무 오류·시스템 오류를 구분하고, 사용자에게는 안전한 메시지를 제공하면서 운영자에게는 원인 예외와 GUID를 남기는 표준 오류처리를 살펴보았다.

이번 장에서는 거래를 실행하기 전에 반드시 확인해야 하는 **사용자의 신원과 권한**을 다룬다.

보안 처리는 다음 한 줄로 끝나지 않는다.

Authorization Header에 JWT가 있다.
→ 토큰을 해석한다.
→ 사용자를 확인한다.

JWT 문자열을 읽을 수 있다는 사실은 그 토큰을 신뢰할 수 있다는 뜻이 아니다.

다음 항목을 모두 검증해야 한다.

서명이 올바른가?

승인된 발급자가 발급했는가?

이 시스템을 대상으로 발급했는가?

만료되지 않았는가?

아직 사용 가능한 토큰인가?

강제로 폐기된 토큰은 아닌가?

JWT의 사용자와 요청 Header의 사용자가 일치하는가?

사용자가 현재도 활성 상태인가?

해당 ServiceId를 실행할 권한이 있는가?

조회·변경하려는 데이터에 접근할 권한이 있는가?

현재 업무 상태에서 그 행위가 허용되는가?

따라서 보안 흐름은 다음과 같이 여러 책임으로 나뉜다.

SSO
→ 사용자의 최초 신원 확인

tcf-jwt
→ Access·Refresh Token 발급·갱신·폐기

Gateway
→ 외부 요청의 JWT 1차 검증

업무 WAR JWT Filter
→ Gateway 우회 가능 환경의 방어 검증

TCF·STF
→ 인증 문맥과 표준 Header 정합성 검증

공통 권한
→ ServiceId 기능 실행권한 확인

업무 Service·Rule
→ 조직·소유자·상태·데이터 범위 권한 확인

OM
→ 사용자·권한·정책·토큰 폐기·감사 관리

프로젝트 기준에서도 JWT는 업무 권한을 대신하지 않는다. JWT는 “누가 요청했는가”를 증명하는 1차 신원 수단이고, STF와 업무 Rule이 거래 실행 가능 여부와 데이터 접근 가능 여부를 추가로 판단한다.

# 문서 개요

## 목적

본 장의 목적은 NSIGHT TCF에서 인증·인가·세션·JWT의 책임을 분리하고 다음 기준을 정의하는 것이다.

SSO 로그인

Access Token 발급

Refresh Token 발급·Rotation

JWT 서명·Claim 검증

Gateway 인증

업무 WAR 직접 접근 방어

AuthenticationContext 생성

표준 Header 정합성

기능권한

데이터권한

세션·Token Family 상태

만료·로그아웃·강제 폐기

키 저장·배포·회전

인증로그·감사로그

장애·침해사고 대응

## 적용범위

전용 브라우저·Web UI

SSO·IdP

tcf-jwt

tcf-gateway

tcf-web JWT Filter

tcf-core·STF

tcf-om

17개 업무 WAR

내부 업무 연계 Client

관리자·운영자 기능

Spring Session JDBC 사용 가능 영역

인증·권한 관련 DB와 감사로그

## 대상 독자

초보·중급 업무 개발자

프레임워크 개발자

애플리케이션 아키텍트

보안 아키텍트

인프라·WAS 운영자

OM 운영관리 담당자

QA·보안 테스트 담당자

감사·내부통제 담당자

## 선행조건

표준 Header 이해

Gateway·TCF 거래 흐름 이해

ServiceId 이해

STF 사전 처리 이해

오류코드·거래로그 이해

HTTP Cookie·Header 기본 이해

공개키·개인키 기본 개념 이해

# 핵심 관점

인증에 성공했다고
업무 권한이 생기는 것은 아니다.

JWT의 서명이 맞다고
사용자가 모든 데이터에 접근할 수 있는 것도 아니다.

신원·기능·데이터·상태 권한을
서로 다른 경계에서 검증해야
실제 업무 보안이 완성된다.

# 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 인증과 인가의 차이를 설명한다. |
| 2 | 기능권한과 데이터권한을 구분한다. |
| 3 | SSO와 JWT의 역할을 구분한다. |
| 4 | Access Token과 Refresh Token을 구분한다. |
| 5 | JWT가 암호화가 아니라 서명된 Claim 집합임을 설명한다. |
| 6 | iss, aud, sub, exp, iat, jti의 의미를 설명한다. |
| 7 | Gateway에서 JWT를 검증하는 흐름을 설명한다. |
| 8 | Gateway가 없을 때 업무 WAR JWT Filter가 필요한 이유를 설명한다. |
| 9 | JWT Claim과 StandardHeader의 정합성을 검증한다. |
| 10 | 사용자 입력 Header를 신뢰하면 안 되는 이유를 설명한다. |
| 11 | STF 공통 권한과 업무 Rule 권한을 구분한다. |
| 12 | SSO 인증 결과로 JWT를 발급하는 흐름을 설명한다. |
| 13 | SSO 내부 호출의 위변조와 재전송을 방지한다. |
| 14 | Refresh Token을 원문으로 저장하면 안 되는 이유를 설명한다. |
| 15 | Refresh Token Rotation과 재사용 탐지를 설명한다. |
| 16 | HttpSession과 JWT 상태의 차이를 설명한다. |
| 17 | 완전 Stateless JWT의 한계를 설명한다. |
| 18 | JWT와 최소 서버 상태를 결합하는 이유를 설명한다. |
| 19 | 동일 Tomcat의 여러 WAR가 자동으로 세션을 공유하지 않음을 설명한다. |
| 20 | 여러 WAR가 같은 공개키를 사용하는 구조를 설명한다. |
| 21 | Private Key가 사용자별·WAR별로 존재하지 않음을 설명한다. |
| 22 | JWKS와 kid를 이용한 키 회전을 설명한다. |
| 23 | 만료·위변조·폐기 Token을 구분한다. |
| 24 | 로그아웃과 강제 로그아웃을 구현한다. |
| 25 | 권한 변경 시 기존 Token 처리정책을 정의한다. |
| 26 | 인증정보를 로그에 안전하게 기록한다. |
| 27 | 인증 저장소·JWKS 장애 시 정책을 설명한다. |
| 28 | 정상·만료·위변조·권한 부족 테스트를 작성한다. |

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| 인증·Authentication | 요청자가 누구인지 확인 |
| 인가·Authorization | 인증된 사용자가 무엇을 할 수 있는지 판단 |
| SSO | 한 번의 인증으로 여러 업무 시스템을 사용하는 구조 |
| IdP | 사용자의 신원을 확인하는 인증 제공자 |
| Access Token | 업무 API 호출에 사용하는 짧은 수명의 Token |
| Refresh Token | Access Token을 재발급받기 위한 긴 수명의 Token |
| JWT | Claim을 포함하고 전자서명된 Token 형식 |
| Claim | Token 안에 담긴 사용자·발급·만료 등의 정보 |
| iss | Token 발급자 |
| aud | Token 사용 대상 |
| sub | Token의 주체 |
| jti | JWT 고유 식별자 |
| iat | 발급시각 |
| exp | 만료시각 |
| JWKS | 검증용 공개키를 제공하는 표준 형식 |
| kid | Token 서명에 사용한 키의 식별자 |
| DenyList | 만료 전이라도 사용을 금지한 Token 목록 |
| Token Family | 로그인 장치·세션 단위의 Refresh Token 계열 |
| Rotation | Refresh Token 사용 시 기존 Token을 폐기하고 새 Token을 발급 |
| AuthenticationContext | 검증된 사용자 신원 정보를 담는 서버 내부 문맥 |
| 기능권한 | ServiceId·메뉴·기능 실행 가능 여부 |
| 데이터권한 | 지점·조직·고객·업무 데이터 접근 범위 |
| Idle Timeout | 일정 시간 활동이 없을 때 만료 |
| Absolute Timeout | 활동 여부와 관계없는 최대 로그인 유지시간 |
| Clock Skew | 서버 시간차를 허용하는 범위 |

# 전체 목표 아키텍처

## Gateway가 있는 경우

┌───────────────────────────────────────────────────────────┐
│ 사용자·전용 브라우저 │
└────────────────────────┬──────────────────────────────────┘
│ SSO Login
▼
┌───────────────────────────────────────────────────────────┐
│ SSO·IdP │
│ 사용자 신원 확인 │
│ SSO Assertion·Code·Token 발급 │
└────────────────────────┬──────────────────────────────────┘
▼
┌───────────────────────────────────────────────────────────┐
│ tcf-om 또는 인증 연계영역 │
│ SSO 결과 검증 │
│ 내부 서명 요청 생성 │
└────────────────────────┬──────────────────────────────────┘
▼
┌───────────────────────────────────────────────────────────┐
│ tcf-jwt │
│ Access Token·Refresh Token 발급 │
│ Private Key 서명 │
│ Refresh Hash·Token Family 저장 │
│ JWKS Public Key 제공 │
└────────────────────────┬──────────────────────────────────┘
│ Access Token
▼
┌───────────────────────────────────────────────────────────┐
│ tcf-gateway │
│ Bearer Token 추출 │
│ 서명·exp·iss·aud 검증 │
│ Header 사용자 정합성 │
│ 업무 Route 결정 │
└────────────────────────┬──────────────────────────────────┘
▼
┌───────────────────────────────────────────────────────────┐
│ 업무 WAR·TCF │
│ AuthenticationContext │
│ STF Header·Claim 정합성 │
│ 기능권한·거래통제 │
└────────────────────────┬──────────────────────────────────┘
▼
┌───────────────────────────────────────────────────────────┐
│ Service·Rule │
│ 지점·소유자·상태·데이터권한 │
└───────────────────────────────────────────────────────────┘

현재 Gateway 기준 인증 흐름은 BusinessRouteService → GRF → GSF → GatewayAuthenticationService → GatewayJwtValidator → 업무 WAR → TCF → STF 순서로 구성된다.

## Gateway가 없는 경우

사용자
│
│ Authorization: Bearer {JWT}
▼
L4·Apache
│
▼
업무 WAR
│
├─ TcfJwtAuthenticationFilter
│ ├─ JWT 서명 검증
│ ├─ exp·iss·aud 검증
│ └─ AuthenticationContext 생성
│
├─ OnlineTransactionController
├─ TCF
├─ STF
│ ├─ Claim·Header 정합성
│ ├─ 기능권한
│ └─ 거래통제
│
└─ Handler → Service → Rule

Gateway가 없더라도 Handler나 Service가 JWT 문자열을 직접 해석해서는 안 된다. HTTP Filter에서 검증을 완료하고, 업무 계층에는 검증된 AuthenticationContext만 전달해야 한다.

# 현재 구현과 목표 구조

## 현재 기준 소스에서 확인되는 기능

| 영역 | 확인된 기능 |
| --- | --- |
| tcf-jwt | 로그인·SSO 발급·Refresh·Logout·Revoke |
| 서명 | RS256 |
| Access Claim | iss, sub, aud, jti, iat, exp, userId, branchId, authGroupId, channelId |
| Refresh Token | Hash 저장 |
| Token Family | Family ID 저장 |
| Refresh Rotation | 기존 Refresh 사용처리 |
| DenyList | jti 폐기 정보 저장 |
| Gateway | Bearer 추출·JwtDecoder 검증 |
| Gateway Claim | 사용자·지점·채널 추출 |
| Header 비교 | JWT 사용자와 요청 Header 사용자 비교 가능 |
| 업무 WAR | TcfJwtAuthenticationFilter 조건부 적용 |
| STF | AuthenticationContext와 Header 정합성 검증 |
| 권한 | 공통 AuthorizationValidator와 업무 Rule 구조 |
| SSO 내부 호출 | 서비스명·Timestamp·서명·허용 IP 검증 |

## 현재 설정 예

현재 로컬 기준 설정에는 다음 값이 존재한다.

Algorithm
RS256

Access Token
15분

Refresh Token
8시간

Clock Skew
60초

DenyList
활성

Refresh Rotation
활성

Issuer
NSIGHT-AUTH

Audience
NSIGHT-MP

이 값은 운영 확정값이 아니라 현재 소스의 기본·로컬 값이다. 보안정책·사용자 경험·성능·감사 요구를 반영해 운영 승인 후 확정해야 한다.

## 현재 구현의 주요 보완점

| 항목 | 상태 | 보완 판단 |
| --- | --- | --- |
| RS256 발급 | 구현 확인 | 유지 |
| JWKS 검증 | 구현 확인 | Cache·장애 정책 보완 |
| Access·Refresh 분리 | 구현 확인 | 유지 |
| Refresh Hash 저장 | 구현 확인 | Salt·보호정책 검토 |
| Refresh Rotation | 구현 확인 | Family 재사용 탐지 보완 |
| DenyList 저장 | 구현 확인 | Gateway·WAR 검증 연계 확인 |
| Header 사용자 비교 | 구현 확인 | 운영 Strict 활성 권장 |
| Header 지점·채널 비교 | STF 구현 확인 | 필수 여부 명확화 |
| 업무 WAR Filter | 구현 확인 | 직접 접근 가능 환경 필수 |
| 서명키 생성 | 현재 기동 시 생성 | **운영 사용 금지** |
| 키 보관 | 보완 필요 | HSM·KMS·Vault |
| kid | 고정값 | 다중 키·회전 구조 필요 |
| Filter 응답 | 단순 JSON | StandardResponse 통일 필요 |
| 인증 로그 | 일부 System.out | 구조화 로그로 교체 |
| 사용자 상태 재확인 | 부분 구현 | 폐기·잠금 연계 보완 |
| Token Version | 확인 필요 | 전체 강제종료에 권장 |
| 권한 변경 즉시 반영 | 보완 필요 | 짧은 Access·Cache Evict·Version |
| Gateway 우회 네트워크 | 프로젝트 확인 필요 | 방화벽·Apache ACL 필수 |

가장 중요한 Gap은 서명키다.

현재 개발용
애플리케이션 기동 시 RSA Key 생성

문제
tcf-jwt 재기동
→ 새로운 Private·Public Key
→ 기존 Access Token 서명 검증 실패
→ 모든 사용자가 즉시 인증 실패

운영환경에서는 서명키를 HSM·KMS·Secret Vault 또는 승인된 Key Store에 영속 보관하고, kid 기반 다중 키 회전을 지원해야 한다. Private Key 관리와 JWKS 공개키 배포는 운영 전 필수 점검 항목이다.

# 22.1 인증과 인가의 차이

## 22.1.1 인증

인증은 요청자의 신원을 확인하는 과정이다.

이 요청을 보낸 사용자는 누구인가?

신원 확인이 완료됐는가?

신원을 증명하는 Token은 신뢰할 수 있는가?

인증 결과 예:

userId = U12345

branchId = 001234

authGroupId = BRANCH\_MANAGER

channelId = WEBTOP

jti = JWT-abc123

인증은 “이 사용자가 U12345다”라는 사실을 확인한다.

## 22.1.2 인가

인가는 인증된 사용자가 요청한 행위를 수행할 수 있는지 판단한다.

U12345는 고객요약 화면을 조회할 수 있는가?

캠페인을 등록할 수 있는가?

다른 지점 고객을 조회할 수 있는가?

본인이 작성한 승인요청을 직접 승인할 수 있는가?

삭제된 데이터를 복구할 수 있는가?

## 22.1.3 인증과 인가 비교

| 구분 | 인증 | 인가 |
| --- | --- | --- |
| 핵심 질문 | 누구인가 | 무엇을 할 수 있는가 |
| 대표 수단 | SSO·JWT·Session | 기능권한·데이터권한·Rule |
| 주요 위치 | IdP·Gateway·JWT Filter | STF·Service·Rule |
| 결과 | AuthenticationContext | 허용·거절 |
| 실패 응답 | 401 계열 | 403·업무 권한 오류 |
| 기준정보 | 사용자·Token | 권한그룹·기능·조직·상태 |
| 캐시 | 공개키·사용자 상태 | 기능·데이터권한 |
| 감사 | 로그인·Token 발급 | 중요 기능·데이터 접근 |

## 22.1.4 기능권한과 데이터권한

기능권한:

이 사용자는
CM.Campaign.approve를 실행할 수 있는가?

데이터권한:

이 사용자는
해당 캠페인 또는 해당 지점 고객을 처리할 수 있는가?

상태권한:

현재 REQUESTED 상태인 캠페인을
이 사용자가 승인할 수 있는가?

권장 검증 순서:

인증
↓
ServiceId 기능권한
↓
대상 데이터 조회
↓
조직·소유자 데이터권한
↓
현재 상태·업무 Rule
↓
실제 처리

## 22.1.5 인증만으로 권한을 판단하면 안 되는 이유

JWT에 authGroupId=MANAGER가 있다고 가정한다.

이 정보만으로 다음을 모두 허용하면 안 된다.

모든 지점 고객 조회

모든 캠페인 승인

개인정보 원문 조회

관리자 정책 변경

삭제 데이터 복구

JWT Claim은 발급 당시의 사용자 속성을 표현한다.

실제 권한은 다음에 따라 달라질 수 있다.

현재 조직

발령·인사 변경

사용자 잠금

권한그룹 변경

업무 데이터 소유자

업무 상태

조회 목적

승인 분리 원칙

## 22.1.6 StandardHeader를 신뢰하면 안 된다

요청 Body:

{
"header": {
"userId": "ADMIN",
"branchId": "999999",
"serviceId": "OM.Security.updatePolicy"
}
}

사용자는 개발자 도구나 직접 HTTP 호출로 Header를 변경할 수 있다.

따라서 다음 값의 신뢰 우선순위는 명확해야 한다.

검증된 JWT Claim
\>
Gateway·Filter가 만든 AuthenticationContext
\>
클라이언트가 보낸 StandardHeader

Header 값은 업무 추적과 계약을 위한 값이며 신원의 최종 근거가 아니다.

## 22.1.7 AuthenticationContext

권장 구조:

public record AuthenticationContext(
String userId,
String branchId,
String channelId,
String jti,
String authGroupId,
Set<String> roles,
String tokenFamilyId,
Instant authenticatedAt
) {}

특징:

서버가 생성한다.

읽기 전용이다.

업무 Request DTO와 분리한다.

Thread·Request 범위로 관리한다.

요청 종료 시 반드시 제거한다.

비동기 Thread로 넘어갈 때 명시적으로 전파한다.

## 22.1.8 Claim과 Header 정합성

JWT.userId
↔ Header.userId

JWT.branchId
↔ Header.branchId

JWT.channelId
↔ Header.channelId

불일치:

JWT userId = U12345

Header userId = ADMIN

처리:

요청 차단

401 또는 인증문맥 불일치 오류

Handler 미실행

보안로그 기록

반복 시 보안관제 알림

## 22.1.9 Fail Closed

권한정보가 없거나 권한 시스템을 조회할 수 없을 때 전체 허용으로 처리하면 안 된다.

권한정보 없음
→ 허용

권한 Cache 오류
→ 허용

OM 권한 DB 장애
→ 허용

금지다.

권장:

인증·권한을 확인할 수 없음
→ 실행 차단
→ 운영 오류

단, 공개 Health Check와 로그인 면제 거래는 명시적 Allow List로 분리한다.

## 22.1.10 로그인 면제 거래

예:

JWT.Auth.login

JWT.Auth.ssoIssue

Health Check

JWKS 조회

면제 기준:

코드에 명시적 등록

OM Catalog 등록

최소 Endpoint

Rate Limit

입력 제한

감사·보안로그

일반 업무 ServiceId 면제 금지

문자열 Prefix로 광범위하게 면제하지 않는다.

## 22.1.11 업무 Rule 권한

public void validateApprovalPermission(
Campaign campaign,
AuthenticationContext auth) {

if (!auth.roles().contains(
"CM\_APPROVER")) {

throw new AuthorizationException(
"E-CM-AUT-0001",
"캠페인 승인 권한이 없습니다."
);
}

if (campaign.createdBy()
.equals(auth.userId())) {

throw new AuthorizationException(
"E-CM-AUT-0002",
"본인이 작성한 캠페인은 직접 승인할 수 없습니다."
);
}

if (!campaign.branchId()
.equals(auth.branchId())) {

throw new AuthorizationException(
"E-CM-AUT-0003",
"해당 조직의 캠페인만 승인할 수 있습니다."
);
}
}

## 22.1.12 인증·인가 책임 경계

| 영역 | 책임 |
| --- | --- |
| SSO·IdP | 최초 사용자 인증 |
| tcf-jwt | Token 발급·갱신·폐기 |
| tcf-gateway | 외부 요청 JWT 검증 |
| tcf-web | 직접 진입 JWT 검증 |
| tcf-core·STF | AuthenticationContext·Header 정합성 |
| OM 기능권한 | ServiceId 실행권한 |
| 업무 Service·Rule | 데이터·상태·소유권 |
| DB | 최종 데이터 무결성 |
| 운영·보안 | 로그인·권한변경·폐기 감사 |

이러한 책임 분리는 프로젝트의 목표 인증 아키텍처와 일치한다.

# 22.2 SSO 로그인과 토큰 발급

## 22.2.1 SSO와 JWT의 차이

SSO
\= 사용자의 신원을 확인하는 인증 절차

JWT
\= 확인된 신원을 업무 시스템에 전달하는 Token 형식

SSO가 JWT를 직접 발급할 수도 있지만, NSIGHT 구조에서는 사내 SSO 결과를 검증한 후 tcf-jwt가 NSIGHT 전용 Token을 발급하는 구조를 사용할 수 있다.

장점:

사내 SSO 계약과 업무 JWT 계약 분리

업무 Claim 표준화

NSIGHT 전용 Audience

Token 수명 독립 관리

강제 폐기·Refresh 정책 적용

업무 시스템별 키 배포 단순화

## 22.2.2 SSO 로그인 전체 흐름

\[사용자\]
SSO 로그인 실행
↓
\[SSO·IdP\]
사용자 인증
↓
Authorization Code·Assertion·SSO Token
↓
\[tcf-om 인증 연계\]
SSO 결과 검증
사용자·조직·권한 조회
↓
내부 서명 요청
↓
\[tcf-jwt\]
JWT.Auth.ssoIssue
↓
Access Token 발급
Refresh Token 발급
Token 상태 저장
↓
\[화면\]
업무 호출

## 22.2.3 SSO 결과 검증

검증 대상:

SSO 발급자

Assertion 서명

만료시각

대상 시스템

Nonce·State

사용자 Subject

재사용 여부

인증 강도

사용자 활성 상태

조직·권한 매핑

SSO 응답 Body의 userId만 보고 Token을 발급해서는 안 된다.

## 22.2.4 JWT.Auth.ssoIssue

대표 ServiceId:

JWT.Auth.ssoIssue

이 거래는 일반 사용자 브라우저가 직접 호출하면 안 된다.

허용
tcf-om 인증 연계 → tcf-jwt

금지
일반 화면 → tcf-jwt ssoIssue 직접 호출

현재 내부 호출 검증은 다음 항목을 확인한다.

내부 호출 Header

허용된 호출 서비스명

요청 Timestamp

허용 Clock Skew

Canonical Body HMAC 서명

허용 IP

이는 SSO 발급 요청의 위변조와 재전송을 줄이기 위한 방어다.

## 22.2.5 내부 호출 보안

권장 Canonical 값:

serviceId

userId

issuer

ssoSubject

ssoAssertionId

branchId

authGroupId

timestamp

서명:

HMAC-SHA256(
sharedSecret,
canonicalBody + timestamp
)

보완 기준:

Shared Secret은 설정파일 평문 금지

Vault·Secret Manager 사용

Secret Rotation

짧은 Timestamp 허용범위

Assertion ID 재사용 방지

mTLS 검토

허용 IP는 보조 통제

IP Allow List만으로 내부 호출을 신뢰하지 않는다.

## 22.2.6 Access Token Claim

현재 대표 Claim:

{
"iss": "NSIGHT-AUTH",
"sub": "U12345",
"aud": \["NSIGHT-MP"\],
"jti": "JWT-78f9...",
"iat": 1784334600,
"exp": 1784335500,
"userId": "U12345",
"userName": "홍길동",
"branchId": "001234",
"authGroupId": "BRANCH\_MANAGER",
"channelId": "WEBTOP"
}

Claim 설계 원칙:

최소 정보만 포함

자주 변하는 대량 권한 목록 제외

민감정보 제외

Token 크기 제한

Claim 의미 문서화

Audience별 분리 검토

포함 금지:

주민등록번호

계좌번호

전화번호

비밀번호

전체 메뉴·고객 목록

개인정보 원문

Refresh Token

## 22.2.7 JWT는 암호화가 아니다

일반적인 서명형 JWT의 Payload는 Base64URL로 인코딩되어 있을 뿐이다.

브라우저나 사용자가
Payload 내용을 읽을 수 있다.

따라서 “서명돼 있으므로 개인정보를 넣어도 된다”는 판단은 잘못이다.

서명은 다음을 증명한다.

Token 내용이 서명 후 변경되지 않았다.

승인된 Private Key로 서명됐다.

서명은 Payload 기밀성을 보장하지 않는다.

## 22.2.8 Access Token

특징:

짧은 수명

업무 API 호출에 사용

매 요청 전송

서명 검증 가능

만료 전 강제 폐기는 별도 상태 필요

유효시간은 다음을 함께 고려한다.

사용자 편의

Token 탈취 위험

Gateway 검증 부하

Refresh 호출량

권한 변경 반영시간

장애 시 재로그인 영향

## 22.2.9 Refresh Token

특징:

Access Token보다 긴 수명

Access Token 재발급에만 사용

업무 API 호출에 사용하지 않음

서버 상태와 연결

유출 시 위험이 큼

원문 저장 금지:

DB
→ SHA-256 등 승인된 Hash만 저장

응답
→ 발급 시 한 번만 전달

로그
→ 원문 금지

프로젝트 기준도 Refresh Token 원문 저장을 금지하고 Hash 저장과 Rotation을 요구한다.

## 22.2.10 Refresh Token Rotation

Refresh Token A 사용
↓
A를 ROTATED 처리
↓
Access Token B 발급
↓
Refresh Token B 발급
↓
같은 Token Family 유지

이후 A가 다시 사용되면 탈취 가능성이 있다.

권장:

기존 Refresh 재사용 감지
↓
해당 Token Family 전체 폐기
↓
사용자 재로그인
↓
보안로그·알림

현재 구현은 이미 사용되거나 Rotation된 Refresh를 거절할 수 있으나, 재사용 탐지 시 Family 전체를 폐기하는 정책은 별도 보완이 필요하다.

## 22.2.11 Token Family

한 사용자가 여러 장치에서 로그인할 수 있다.

사용자 U12345
├─ 지점 PC
│ └─ Token Family F1
├─ 관리자 PC
│ └─ Token Family F2
└─ 테스트 단말
└─ Token Family F3

관리 기능:

특정 장치 로그아웃
→ F1 폐기

모든 장치 로그아웃
→ F1·F2·F3 폐기
또는 Token Version 증가

## 22.2.12 로그인 응답

{
"accessToken": "{JWT}",
"refreshToken": "{opaque-token}",
"tokenType": "Bearer",
"expiresIn": 900,
"issuer": "NSIGHT-AUTH",
"audience": "NSIGHT-MP",
"jti": "JWT-78f9..."
}

운영 로그에서는 다음만 기록한다.

userId

tokenFamilyId

jti

issuer

audience

expiresAt

clientIp

deviceId·UserAgent Hash

result

Token 원문은 기록하지 않는다.

## 22.2.13 브라우저 Token 저장

대안:

| 방식 | 장점 | 위험 |
| --- | --- | --- |
| JavaScript Memory | XSS 지속탈취 감소 | 새로고침 시 소실 |
| localStorage | 구현 단순 | XSS에 취약 |
| sessionStorage | 탭 단위 | XSS에 취약 |
| HttpOnly Cookie | JavaScript 접근 차단 | CSRF 방어 필요 |
| BFF Session | 브라우저에 Token 미노출 | 서버 상태·구성 증가 |
| 전용 브라우저 Secure Store | 통제 가능 | 제품 연계 필요 |

권장 원칙:

Refresh Token
→ localStorage 저장 금지 권장
→ HttpOnly·Secure Cookie 또는 전용 Secure Store 검토

Access Token
→ 짧은 수명
→ Memory 또는 통제된 Cookie

Cookie를 사용한다면 다음을 적용한다.

HttpOnly

Secure

SameSite

Cookie Path

Cookie Domain

CSRF Token·Origin 검증

## 22.2.14 Token 발급키

Private Key
→ tcf-jwt만 보유
→ Token 서명

Public Key
→ JWKS로 공개
→ Gateway·업무 WAR 검증

Private Key는 다음에 두지 않는다.

업무 WAR

OM WAR

UI

Git 저장소

application.yml

로그

사용자별 DB Row

## 22.2.15 Private Key는 사용자별인가

아니다.

사용자 36,000명
→ Private Key 36,000개

구조가 아니다.

일반적으로 발급 서비스의 서명키가 Token을 서명한다.

tcf-jwt 발급키
↓
여러 사용자의 Token 서명

사용자별 구분은 sub, userId, jti, Token Family로 한다.

## 22.2.16 Public Key는 업무 WAR별인가

기본적으로 같은 발급자와 같은 Audience 정책을 사용하는 WAR는 같은 JWKS의 공개키를 검증한다.

tcf-jwt JWKS
├─ Gateway
├─ SV WAR
├─ IC WAR
├─ CM WAR
└─ OM WAR

각 WAR에 별도의 JwtDecoder와 JWKS Cache가 생성될 수 있지만 공개키 자체를 WAR별·사용자별로 별도 발급할 필요는 없다.

동일 Tomcat에 여러 WAR가 배포되어도 각 WAR의 Spring Context는 독립적이며, JWT 검증기는 Context별로 존재할 수 있다.

## 22.2.17 키 회전

정상 회전:

기존 키
kid=key-2026-01

신규 키 배포
kid=key-2026-07
↓
JWKS에 두 공개키 제공
↓
신규 Token은 key-2026-07로 서명
↓
기존 Access Token 만료 대기
↓
기존 공개키 제거

필수 조건:

JWKS 다중 키

고유 kid

키 활성·폐기 시각

Gateway Cache 갱신

구키 유지기간

Rollback

감사로그

긴급 폐기 절차

# 22.3 세션·JWT 하이브리드 구조

## 22.3.1 HttpSession

HttpSession은 서버가 로그인 상태를 저장하는 방식이다.

브라우저
→ JSESSIONID

서버
→ Session ID로 사용자 상태 조회

장점:

강제 종료 쉬움

서버에서 상태 즉시 변경

민감정보를 브라우저에 덜 노출

전통적 웹 구조에 단순

주의:

다중 서버 세션 공유

Failover

세션 저장용량

Serialization

DB·Redis 장애

Sticky Session

## 22.3.2 JWT

JWT는 요청마다 서명된 Token을 전달하는 방식이다.

장점:

업무 서버가 사용자 Session 객체를 직접 공유하지 않아도 됨

다중 WAR·다중 인스턴스 확장 용이

표준 Claim 전달

Gateway 검증 가능

주의:

만료 전 강제 폐기 어려움

권한 변경 즉시 반영 어려움

Token 탈취 대응 필요

Refresh 상태관리 필요

큰 Token의 네트워크 비용

## 22.3.3 완전 Stateless JWT의 한계

완전 Stateless는 서버가 Token 상태를 전혀 저장하지 않는 구조다.

서명과 만료가 유효하면
항상 사용 가능

문제:

로그아웃 후 Access Token 계속 사용 가능

퇴직·잠금 사용자의 Token 사용 가능

권한 변경 즉시 반영 어려움

탈취 Token 강제 차단 어려움

Refresh 재사용 탐지 불가

활성 로그인 조회 불가

## 22.3.4 권장 구조: JWT + 최소 상태

프로젝트의 권장 방향은 HttpSession 의존을 제거하더라도 다음 인증 상태는 유지하는 것이다.

Refresh Token Hash

Token Family

DenyList

Token Version

사용자 활성·잠금 상태

권한 변경시각

로그인·폐기 이력

즉 다음 구조다.

HTTP 요청 처리
→ Stateless Access Token 중심

보안 통제
→ 최소 서버 상태 유지

OM의 HttpSession은 제거하더라도 Token Family·Refresh Token·Token Version·DenyList와 사용자 상태를 유지하는 방식이 권장안이다.

## 22.3.5 기존 세션 개념의 전환

| 기존 Session 개념 | JWT+상태 구조 |
| --- | --- |
| Session ID | Token Family ID |
| 세션 사용자 | 활성 Token Family 사용자 |
| Session Idle Timeout | Refresh Idle Timeout |
| Session Absolute Timeout | Token Family Absolute Timeout |
| 세션 강제종료 | Family 폐기 |
| 사용자 전체 세션 종료 | Token Version 증가 |
| 세션 접속 IP | Family Client IP |
| Session User-Agent | Device·User-Agent Hash |
| 세션 조회 화면 | 활성 로그인 현황 |
| Session Store | Token 상태 저장소 |

## 22.3.6 다중 WAR와 HttpSession

동일 Tomcat에 sv.war와 ic.war가 배포돼도 세션은 자동 공유되지 않는다.

/sv HttpSession
≠
/ic HttpSession

이유:

WAR별 Servlet Context

WAR별 Session Manager

Cookie Path

WAR별 ClassLoader

Principal Serialization 차이

같은 JSESSIONID라는 Cookie 이름을 사용한다는 사실만으로 같은 Session이 아니다.

## 22.3.7 세션 공유가 필요한 경우

대안:

Spring Session JDBC

Redis Session

공통 BFF

Gateway Session

중앙 인증 Context 조회

세션을 공유하려면 다음이 일치해야 한다.

Cookie Domain·Path

Session Namespace

Serialization 형식

Principal 모델

Timeout

암호화

정리 Job

DR 복제

Tomcat Cross Context나 WAR 간 Java 객체 직접 공유에 의존하지 않는다.

## 22.3.8 Spring Session JDBC

적합한 상황:

기존 세션 기반 화면 유지

다중 WAS Failover 필요

Redis 사용 불가

세션 강제 종료 필요

서버 상태 기반 제어 필요

주의:

Session 조회 DB 부하

36,000명 규모 세션 용량

60분 Timeout

만료 Session 정리

인덱스

직렬화 크기

DB 장애 영향

세션에 대형 업무 데이터를 저장하지 않는다.

사용자 신원

최소 권한 식별자

로그인 시각

장치 정보

만 저장한다.

## 22.3.9 하이브리드 사용 유형

### 유형 A: Gateway Session + 업무 JWT

브라우저
→ Gateway Session Cookie

Gateway
→ 내부 JWT·AuthenticationContext

업무 WAR
→ JWT 검증

### 유형 B: 브라우저 JWT + 최소 상태

브라우저
→ Access Token

Gateway·WAR
→ JWT 검증

중앙 저장소
→ Refresh·DenyList·Token Version

### 유형 C: BFF Session

브라우저
→ HttpOnly Session Cookie

BFF
→ Access Token 보관

업무 API
→ JWT

### 유형 D: 완전 Session

브라우저
→ JSESSIONID

각 업무
→ 중앙 Session Store

## 22.3.10 대안 비교

| 대안 | 설명 | 판단 |
| --- | --- | --- |
| Session 유지 | 모든 업무가 공유 Session 사용 | 전환·레거시 상황 |
| 완전 Stateless JWT | 상태 저장 없음 | 보안통제 부족 |
| JWT + 최소 상태 | Access는 Stateless, 폐기·Refresh는 상태 | 권장 |
| OM만 Session | 업무와 인증방식 혼재 | 비권장 |
| 업무별 JWT 발급 | WAR마다 Private Key | 금지 |
| Gateway만 검증 | 직접 접근 가능하지만 WAR 검증 없음 | 금지 |
| BFF + JWT | 브라우저 Token 노출 최소화 | 환경에 따라 권장 |

## 22.3.11 Gateway 우회 방지

1차 원칙은 네트워크 통제다.

클라이언트
→ Gateway 접근 허용

Gateway
→ 업무 WAR 접근 허용

클라이언트
→ 업무 WAR 직접 접근 차단

추가 방어:

업무 WAR TcfJwtAuthenticationFilter

mTLS

Gateway 전용 내부 Header 서명

Source IP Allow List

Apache ACL

Security Group

Gateway를 우회해 업무 WAR에 직접 접근할 수 있는 구조에서는 공통 JWT Filter가 필요하다.

## 22.3.12 Gateway가 검증했는데 WAR도 검증해야 하는가

네트워크에서 Gateway 우회가 완전히 차단되고, Gateway와 WAR 간 통신이 신뢰 가능한 경우 중복 서명 검증을 생략할 수 있다.

그러나 다음 가능성이 있으면 WAR 검증이 필요하다.

내부망 직접 접근

운영자 Curl

Apache Route 오설정

다른 업무 WAR의 직접 호출

L4 우회

테스트 Endpoint 노출

방어 수준은 위협모델과 성능시험으로 결정하고 ADR에 기록한다.

## 22.3.13 Token 전달과 Header 보정

Gateway는 검증된 Claim을 기반으로 서버 내부 Header를 보정할 수 있다.

JWT userId
→ Header.userId

JWT branchId
→ Header.branchId

JWT channelId
→ Header.channelId

클라이언트 Header를 그대로 전달하는 것보다 안전하다.

그러나 Gateway가 Header를 보정했다는 사실만으로 업무 데이터권한까지 판단하지 않는다.

## 22.3.14 권한 Cache

기능권한 조회를 매 요청 DB에서 수행하면 부하가 발생할 수 있다.

Cache Key 예:

userId

authGroupId

permissionVersion

businessCode

무효화 조건:

권한그룹 변경

사용자 잠금

조직 이동

메뉴·기능권한 변경

사용자 퇴직

긴급 권한 회수

권한 Cache 장애 시 전체 허용하지 않는다.

## 22.3.15 권한 변경과 기존 Access Token

대안:

### 짧은 Access Token 수명

최대 15분 뒤 신규 권한 반영

### Token Version

JWT tokenVersion=5

사용자 현재 tokenVersion=6
→ 기존 Token 거절

### authChangedAt

JWT.iat < user.authChangedAt
→ 재인증 요구

### DenyList

기존 jti 개별 폐기

권한 중요도에 따라 조합한다.

# 22.4 만료·위변조·권한 부족 처리

## 22.4.1 인증 실패 유형

| 유형 | 예 |
| --- | --- |
| Token 누락 | Authorization Header 없음 |
| 형식 오류 | Bearer Prefix 오류 |
| 서명 오류 | Token 내용 변경 |
| 만료 | exp 경과 |
| 발급자 오류 | iss 불일치 |
| 대상 오류 | aud 불일치 |
| 필수 Claim 누락 | userId 없음 |
| 폐기 Token | DenyList |
| 사용자 잠금 | 현재 사용자 상태 사용 불가 |
| Header 불일치 | JWT와 전문 사용자 불일치 |
| Refresh 재사용 | Rotation된 Token 재사용 |
| 키 오류 | JWKS에 kid 없음 |
| 시간 오류 | 서버 Clock 차이 초과 |

## 22.4.2 Access Token 만료

업무 요청
↓
Gateway JwtDecoder
↓
exp 만료 확인
↓
401
↓
업무 WAR·TCF 미실행

화면:

Access Token 만료
↓
Refresh Token으로 갱신
↓
신규 Access Token
↓
원 요청 1회 재시도

무제한 자동 재시도하지 않는다.

## 22.4.3 Refresh Token 만료

Refresh Token 만료
↓
Token 재발급 불가
↓
Token Family 종료
↓
SSO 재로그인

사용자 메시지:

로그인 시간이 만료되었습니다.
다시 로그인해 주세요.

## 22.4.4 Token 위변조

공격자가 Payload의 userId를 변경했다.

기존
userId=U12345

변조
userId=ADMIN

서명이 더 이상 일치하지 않는다.

JwtDecoder
→ Signature 검증 실패
→ 401

기록:

발생시각

Source IP

Channel

Token kid

검증 실패 유형

Target Route

TraceId

Token 원문은 기록하지 않는다.

## 22.4.5 Issuer 오류

JWT iss
UNKNOWN-AUTH

설정 issuer
NSIGHT-AUTH

다른 시스템이 발급한 Token을 NSIGHT에서 사용하려는 상황일 수 있다.

처리:

401

업무 미실행

인증 실패 Metric

반복 시 보안 경보

## 22.4.6 Audience 오류

JWT aud
NSIGHT-ADMIN

호출 대상
NSIGHT-MP

관리자용 Token으로 일반 업무 API를 호출하거나 반대 상황을 차단한다.

Audience를 모든 시스템이 하나의 값으로 공유하면 Token 오용 범위가 커질 수 있다.

필요하면 다음처럼 분리한다.

NSIGHT-MP

NSIGHT-OM

NSIGHT-JWT-ADMIN

NSIGHT-BATCH

## 22.4.7 Clock Skew

서버 시간차 때문에 발급 직후 또는 만료 직전에 Token이 실패할 수 있다.

발급 서버
10:00:00

Gateway
09:59:20

Clock Skew를 제한적으로 허용할 수 있다.

주의:

Clock Skew를 지나치게 크게 설정
→ 만료 Token 유효기간 연장

NTP 시간 동기화가 우선이다.

## 22.4.8 DenyList

사용 사례:

사용자 로그아웃

Token 탈취

관리자 강제종료

사용자 퇴직

비밀번호 변경

권한 긴급 회수

검증:

JWT 서명 유효
↓
jti DenyList 조회
↓
등록됨
↓
401

현재 소스에는 DenyList 저장 기능이 존재하지만, 실제 Gateway·각 업무 WAR의 매 요청 검증 경로에서 DenyList를 조회하는지는 운영 구현을 별도로 확인해야 한다.

## 22.4.9 DenyList 성능

매 요청 DB 조회:

36,000 사용자
× 동시 요청
→ 인증 DB 부하

대안:

DenyList Cache

짧은 Access Token

Bloom Filter 보조

Token Version Cache

Gateway 중앙 확인

정합성이 중요한 폐기정보이므로 Cache TTL과 전파지연을 정의한다.

## 22.4.10 로그아웃

정상 로그아웃:

Access Token jti 폐기

Refresh Token 폐기

Token Family 상태 종료

로그아웃 이력 기록

브라우저 Token 제거

관련 Cookie 만료

화면에서 Token만 삭제하면 서버 강제 종료가 아니다.

## 22.4.11 강제 로그아웃

### 특정 장치 종료

Token Family F1 폐기

### 사용자 전체 종료

모든 Family 폐기
또는
Token Version 증가

### 특정 Access Token 종료

jti DenyList

관리자 강제종료는 감사 대상이다.

관리자

대상 사용자

대상 Family

사유

처리시각

결과

## 22.4.12 Refresh 재사용 공격

정상 사용자
Refresh A 사용
→ Refresh B 발급

공격자
탈취한 Refresh A 재사용

A는 이미 Rotation 상태다.

권장 처리:

재사용 탐지

→ 해당 Family 전체 폐기

→ 신규 Access도 폐기 검토

→ 사용자 재로그인

→ 보안 알림

→ 장치·IP 확인

단순히 A 요청만 거절하고 B를 유지하면 공격 여부를 놓칠 수 있다.

## 22.4.13 권한 부족

기능권한 부족:

사용자
→ CM.Campaign.approve 호출

권한
→ CM\_APPROVER 없음

결과
→ Handler 전 또는 업무 초기 차단

데이터권한 부족:

사용자 지점
001234

대상 데이터 지점
009999

결과
→ 업무 Rule 권한 오류

사용자 메시지:

해당 기능을 수행할 권한이 없습니다.

대상 존재 여부를 숨겨야 하는 경우:

요청한 데이터를 찾을 수 없거나 접근할 수 없습니다.

## 22.4.14 인증과 권한 오류 응답

### 인증 실패

HTTP/1.1 401 Unauthorized

{
"errorCode": "E-JWT-AUT-0001",
"message": "로그인이 필요합니다."
}

### 권한 실패

HTTP/1.1 403 Forbidden

또는 TCF 표준 업무 실패 정책을 사용할 수 있다.

중요한 것은 Gateway·업무 WAR·TCF에서 서로 다른 계약을 사용하지 않는 것이다.

## 22.4.15 Filter 오류 응답 표준화

현재 JWT Filter가 단순 JSON을 직접 작성하면 TCF 표준 응답과 구조가 달라질 수 있다.

목표:

{
"header": {
"guid": "G-20260718-000501"
},
"result": {
"resultCode": "E0001",
"errorCode": "E-JWT-AUT-0001",
"errorMessage": "로그인이 필요합니다.",
"errorDetail": null
},
"body": null
}

TCF 이전 오류라도 GUID·발생시각·표준 오류코드가 필요하다.

## 22.4.16 JWKS 장애

Gateway가 JWKS를 가져오지 못했다.

### 이미 Cache된 키가 있음

Cache된 공개키로 검증 지속

Cache 만료 전 경보

신규 kid Token은 실패 가능

### Cache가 없음

검증 불가

Fail Closed

503 또는 인증 인프라 오류

업무 요청 차단

공개키를 가져오지 못했다고 서명검증을 생략해서는 안 된다.

## 22.4.17 인증 저장소 장애

Refresh Token DB·DenyList DB·사용자 상태 DB 장애:

Access Token 서명은 유효

하지만
폐기·사용자 상태를 확인할 수 없음

정책 대안:

| 거래 | 정책 |
| --- | --- |
| 일반 조회 | 제한적 Cache 사용 검토 |
| 개인정보 원문 | Fail Closed |
| 등록·변경·승인 | Fail Closed |
| 관리자 정책 | Fail Closed |
| Health | 별도 Endpoint |
| Refresh | 발급 차단 |

보안 수준별 정책을 문서화한다.

## 22.4.18 Private Key 유출

긴급 절차:

1\. 사고 선언
2\. 유출 키 kid 식별
3\. 해당 키 신규 서명 중지
4\. 신규 키 활성화
5\. 구키 공개키 폐기 시점 결정
6\. 기존 Token 전체 폐기
7\. Token Version 증가
8\. 사용자 재로그인
9\. 접근로그·감사 분석
10\. 원인 제거·키 재발급

기존 공개키를 즉시 JWKS에서 제거하면 모든 기존 Token이 실패한다.

침해사고에서는 의도된 조치일 수 있다.

## 22.4.19 Header 위변조

JWT userId
U12345

Header userId
ADMIN

처리:

AuthenticationContextValidator

→ Claim mismatch

→ 거래 차단

→ 보안로그

→ 반복 Source IP 경보

현재 STF는 JWT Claim과 Header의 사용자·지점·채널 정합성을 검증할 수 있는 구조를 가진다.

# 정상 처리 흐름

1\. 사용자가 사내 SSO에 로그인한다.
2\. SSO가 사용자 신원을 확인한다.
3\. 인증 연계영역이 SSO 결과를 검증한다.
4\. 인증 연계영역이 서명된 내부 요청으로 tcf-jwt를 호출한다.
5\. tcf-jwt가 사용자 활성 상태와 조직정보를 확인한다.
6\. Access Token에 표준 Claim을 생성한다.
7\. Private Key로 RS256 서명한다.
8\. Access Token 상태와 Refresh Hash를 저장한다.
9\. 화면에 Token Pair를 반환한다.
10\. 화면이 Access Token으로 Gateway를 호출한다.
11\. Gateway가 서명·만료·발급자·대상을 검증한다.
12\. Gateway가 AuthenticationContext를 생성한다.
13\. 업무 WAR가 TCF에 진입한다.
14\. STF가 Claim과 StandardHeader를 비교한다.
15\. STF가 ServiceId 기능권한을 확인한다.
16\. Service·Rule이 데이터권한과 상태권한을 확인한다.
17\. 업무 거래가 실행된다.
18\. 거래로그와 감사로그에 사용자·ServiceId·결과를 기록한다.

# Access Token 만료 흐름

업무 요청
↓
Gateway
↓
Access Token 만료
↓
401
↓
화면 Refresh 요청
↓
Refresh Hash·상태·만료 확인
↓
기존 Refresh Rotation
↓
신규 Token Pair 발급
↓
원 업무 요청 1회 재시도

# Refresh 재사용 흐름

Rotation된 Refresh Token 재사용
↓
재사용 공격 의심
↓
Token Family 전체 폐기
↓
Access Token 폐기·Version 증가 검토
↓
사용자 재로그인
↓
보안관제 알림

# 권한 변경 흐름

관리자
→ 사용자 승인권한 제거
↓
권한 DB 변경
↓
권한 Cache Evict
↓
authChangedAt 또는 tokenVersion 증가
↓
기존 Access Token 요청
↓
Token 상태 불일치
↓
재인증 요구

# Gateway 우회 흐름

공격자
→ 업무 WAR /sv/online 직접 호출
↓
TcfJwtAuthenticationFilter
↓
Bearer 없음·위변조
↓
401
↓
TCF 미실행

# 정상 예시

사용자
U12345

SSO
인증 성공

Access Token
iss=NSIGHT-AUTH
aud=NSIGHT-MP
userId=U12345
branchId=001234
exp=유효
jti=JWT-001

Gateway
서명·iss·aud·exp 검증 성공

Header
userId=U12345
branchId=001234

STF
Claim·Header 일치

기능권한
SV.Customer.selectSummary 허용

데이터권한
001234 지점 고객 허용

결과
SUCCESS

# 금지 예시

JWT Payload를 읽기만 하고 서명을 검증하지 않는다.

만료시각만 확인하고 issuer·audience를 확인하지 않는다.

클라이언트가 보낸 userId를 인증 사용자로 사용한다.

JWT가 있으면 모든 ServiceId를 허용한다.

Gateway 인증 결과를 데이터권한으로 간주한다.

Handler가 Authorization Header를 직접 파싱한다.

업무 Service마다 서로 다른 JWT 라이브러리를 사용한다.

Gateway 우회가 가능한데 업무 WAR Filter를 끈다.

인증 실패 시 업무를 계속 실행한다.

JWKS 장애 시 서명검증을 생략한다.

Private Key를 업무 WAR에 배포한다.

Private Key를 Git과 application.yml에 저장한다.

Private Key를 사용자별로 생성한다.

업무 WAR별로 독자적인 발급키를 생성한다.

tcf-jwt 재기동마다 운영 키를 새로 만든다.

Refresh Token 원문을 DB에 저장한다.

Refresh Token을 로그에 남긴다.

Refresh Token을 localStorage에 장기 저장한다.

Rotation된 Refresh 재사용을 단순 오류로만 처리한다.

로그아웃 시 브라우저 Token만 삭제한다.

권한 변경 후 기존 Token을 무기한 허용한다.

DenyList 저장만 하고 검증경로에서 조회하지 않는다.

JWT 원문을 System.out으로 출력한다.

인증 오류 응답에 Token 검증 Stack Trace를 넣는다.

권한정보 조회 실패 시 전체 허용한다.

Health Check 면제를 업무 Prefix 전체에 적용한다.

관리자 강제 로그아웃을 감사하지 않는다.

# 표준 데이터 구조

## Token 상태 테이블 예

CREATE TABLE TCF\_JWT\_TOKEN (
TOKEN\_ID VARCHAR2(50) NOT NULL,
JTI VARCHAR2(100) NOT NULL,
ISSUER VARCHAR2(100) NOT NULL,
USER\_ID VARCHAR2(50) NOT NULL,
BRANCH\_ID VARCHAR2(30),
CHANNEL\_ID VARCHAR2(30),
AUTH\_GROUP\_ID VARCHAR2(50),
TOKEN\_TYPE VARCHAR2(20) NOT NULL,
AUDIENCE VARCHAR2(100) NOT NULL,
ISSUED\_AT TIMESTAMP NOT NULL,
EXPIRES\_AT TIMESTAMP NOT NULL,
REVOKED\_YN CHAR(1) DEFAULT 'N' NOT NULL,
REVOKED\_AT TIMESTAMP,
REVOKE\_REASON VARCHAR2(500),
CLIENT\_IP VARCHAR2(64),
USER\_AGENT\_HASH VARCHAR2(128),
CONSTRAINT PK\_TCF\_JWT\_TOKEN
PRIMARY KEY (TOKEN\_ID),
CONSTRAINT UK\_TCF\_JWT\_TOKEN\_01
UNIQUE (ISSUER, JTI)
);

## Refresh Token 예

CREATE TABLE TCF\_REFRESH\_TOKEN (
REFRESH\_TOKEN\_ID VARCHAR2(50) NOT NULL,
TOKEN\_HASH VARCHAR2(128) NOT NULL,
TOKEN\_FAMILY\_ID VARCHAR2(50) NOT NULL,
USER\_ID VARCHAR2(50) NOT NULL,
ISSUED\_AT TIMESTAMP NOT NULL,
LAST\_USED\_AT TIMESTAMP,
EXPIRES\_AT TIMESTAMP NOT NULL,
ROTATED\_YN CHAR(1) DEFAULT 'N' NOT NULL,
REVOKED\_YN CHAR(1) DEFAULT 'N' NOT NULL,
REVOKED\_AT TIMESTAMP,
CLIENT\_IP VARCHAR2(64),
USER\_AGENT\_HASH VARCHAR2(128),
CONSTRAINT PK\_TCF\_REFRESH\_TOKEN
PRIMARY KEY (REFRESH\_TOKEN\_ID),
CONSTRAINT UK\_TCF\_REFRESH\_TOKEN\_01
UNIQUE (TOKEN\_HASH)
);

## Token Family 예

CREATE TABLE TCF\_TOKEN\_FAMILY (
TOKEN\_FAMILY\_ID VARCHAR2(50) NOT NULL,
USER\_ID VARCHAR2(50) NOT NULL,
DEVICE\_ID VARCHAR2(100),
FAMILY\_STATUS VARCHAR2(20) NOT NULL,
TOKEN\_VERSION NUMBER(10) NOT NULL,
CREATED\_AT TIMESTAMP NOT NULL,
LAST\_REFRESH\_AT TIMESTAMP,
IDLE\_EXPIRE\_AT TIMESTAMP,
ABSOLUTE\_EXPIRE\_AT TIMESTAMP,
REVOKED\_AT TIMESTAMP,
REVOKE\_REASON VARCHAR2(500),
CONSTRAINT PK\_TCF\_TOKEN\_FAMILY
PRIMARY KEY (TOKEN\_FAMILY\_ID)
);

# 책임 경계와 RACI

| 활동 | SSO | JWT | Gateway | FW·STF | 업무개발 | OM·운영 | 보안 | QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 사용자 최초 인증 | A/R | I | I | I | I | C | A/C | C |
| JWT Claim 설계 | C | R | C | C | C | C | A | C |
| Token 발급 | I | A/R | I | I | I | C | C | C |
| Refresh·Rotation | I | A/R | I | I | I | R/C | C | C |
| Private Key | I | R | I | I | I | C | A/R | C |
| JWKS 검증 | I | C | A/R | R/C | I | C | C | C |
| Header 정합성 | I | I | R/C | A/R | I | I | C | C |
| 기능권한 | I | I | C | R | C | A/R | C | C |
| 데이터권한 | I | I | I | C | A/R | C | C | C |
| 세션·Family 정책 | I | R | C | C | I | A/R | A/C | C |
| 강제 로그아웃 | I | R | C | I | I | A/R | C | C |
| 인증 감사 | C | R | R | C | C | A/R | A/C | C |
| 장애 대응 | C | R | R | R | C | A/R | A/C | C |

# 데이터 및 상태관리

## Token 상태

ACTIVE

ROTATED

REVOKED

EXPIRED

COMPROMISED

## Token Family 상태

ACTIVE

IDLE\_EXPIRED

ABSOLUTE\_EXPIRED

REVOKED

COMPROMISED

CLOSED

## 사용자 상태

ACTIVE

LOCKED

SUSPENDED

RETIRED

DELETED

Token의 서명이 유효해도 사용자가 LOCKED 또는 RETIRED라면 중요 거래를 거절해야 한다.

# 성능·용량·확장성

## JWT 검증 비용

서명검증은 DB 조회 없이 가능하지만 다음 비용이 있다.

RSA 서명검증 CPU

JWKS Cache

Token Parsing

Claim 검증

Header 비교

일반적으로 업무 SQL보다 작지만 전체 요청에 적용되므로 성능시험이 필요하다.

## DenyList·권한 상태 조회

매 요청 DB 조회 시:

동시요청
× DenyList
× 사용자 상태
× 권한

부하가 증가할 수 있다.

대안:

Gateway 중앙 Cache

짧은 Access Token

Token Version Cache

이벤트 기반 Cache Evict

권한 Snapshot

## Session JDBC 용량

현재 프로젝트 전제:

최대 사용자
36,000명

세션 Timeout
60분

산정 대상:

평균 Session 크기

동시 로그인 수

Session Attribute 수

DB Read·Write TPS

만료 정리 TPS

장애 복구시간

센터 간 복제

세션에 업무 조회결과를 넣지 않는 것이 중요하다.

## JWKS Cache

정상
→ Cache된 공개키 사용

신규 kid
→ JWKS 갱신

JWKS 장애
→ 기존 Cache 유지

Cache 없음
→ 인증 차단

키 회전 전에 Gateway와 모든 WAR의 JWKS Cache 동작을 검증해야 한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| Access Token | 원문 로그 금지 |
| Refresh Token | 원문 DB·로그 금지 |
| Private Key | HSM·KMS·Vault |
| Public Key | JWKS 제공 |
| Claim | 최소정보 |
| Header | Claim과 정합성 검증 |
| 사용자 상태 | 잠금·퇴직 즉시 반영 |
| 권한 | 기능·데이터·상태 분리 |
| 강제종료 | 관리자 감사 |
| SSO 발급 | Assertion 재사용 방지 |
| 내부 호출 | HMAC·Timestamp·IP·mTLS |
| 로그인 실패 | 계정 존재 여부 숨김 |
| Token 탈취 | Family 폐기·경보 |
| 로그 | jti·userId 중심, Token 제외 |

# 운영·모니터링·장애 대응

## 권장 Metric

auth.login.success.count

auth.login.fail.count

auth.jwt.validation.fail.count

auth.jwt.expired.count

auth.jwt.signature.fail.count

auth.jwt.issuer.fail.count

auth.jwt.audience.fail.count

auth.jwt.headerMismatch.count

auth.refresh.success.count

auth.refresh.fail.count

auth.refresh.reuse.count

auth.token.revoked.count

auth.authorization.denied.count

auth.jwks.fetch.fail.count

auth.session.active.count

## 권장 인증 로그

event=JWT\_VALIDATION\_FAILED
traceId=T-20260718-000501
businessCode=SV
serviceId=SV.Customer.selectSummary
errorCode=E-JWT-AUT-0004
failureReason=EXPIRED
issuer=NSIGHT-AUTH
kid=key-2026-07
sourceIp=10.10.10.25
instanceId=gateway-01

기록 금지:

token={전체 JWT}

refreshToken={원문}

privateKey={키}

authorizationHeader={전체 값}

## 인증 장애 점검 순서

1\. HTTP 상태 확인
2\. Gateway Access 로그 확인
3\. 인증 실패유형 확인
4\. Token exp·iss·aud·kid 확인
5\. JWKS Cache 확인
6\. tcf-jwt 상태 확인
7\. DenyList·Token Family 확인
8\. 사용자 활성·잠금 확인
9\. Header Claim 불일치 확인
10\. 권한 Cache·OM 권한 확인
11\. 업무 Rule 데이터권한 확인

# 자동검증 및 품질 Gate

## 1\. JWT 설정 Gate

Algorithm = 승인된 값

issuer 필수

audience 필수

JWKS URI 필수

Access 수명 상한

Refresh 수명 상한

Clock Skew 상한

DenyList 정책

Rotation 정책

## 2\. Private Key Gate

배포 Artifact와 저장소에서 다음을 검사한다.

BEGIN PRIVATE KEY

PKCS8

JKS·P12 평문 비밀번호

application.yml Private Key

Git History Key

발견 시 배포를 차단한다.

## 3\. Claim Gate

필수 Claim:

iss

sub 또는 userId

aud

jti

iat

exp

민감 Claim 금지 검사도 수행한다.

## 4\. Gateway 우회 Gate

운영에서 Gateway Route 존재

업무 WAR 직접 접근 ACL

직접 접근 가능 시 JWT Filter 활성

required-for-online=true

인증 면제 ServiceId Allow List

## 5\. Header 정합성 Gate

JWT userId ↔ Header userId

JWT branchId ↔ Header branchId

JWT channelId ↔ Header channelId

운영에서 검증 설정이 비활성화되지 않도록 한다.

## 6\. Refresh Gate

원문 DB 저장 금지

Rotation 활성

기존 Token 재사용 차단

Family 전체 폐기 정책

만료 확인

사용자 활성상태 확인

## 7\. Logging Gate

Authorization Header 출력 금지

JWT 원문 출력 금지

Refresh Token 출력 금지

Private Key 출력 금지

System.out 금지

사용자정보 마스킹

## 8\. 권한 Gate

ServiceId 기능권한 존재

데이터권한 Rule 존재

권한정보 null 시 Fail Closed

작성자·승인자 분리

삭제·복구 관리자 권한

## 9\. 키 회전 Gate

다중 kid JWKS

신규 키 발급

구키 Token 검증

Gateway Cache 갱신

Rollback

감사로그

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| SEC-001 | 정상 SSO 로그인 | Token Pair 발급 |
| SEC-002 | SSO 서명 오류 | 발급 차단 |
| SEC-003 | SSO Assertion 만료 | 발급 차단 |
| SEC-004 | SSO Assertion 재사용 | 차단 |
| SEC-005 | 허용되지 않은 내부 서비스 | 차단 |
| SEC-006 | 내부 Timestamp 만료 | 차단 |
| SEC-007 | 내부 HMAC 오류 | 차단 |
| SEC-008 | 허용 IP 아님 | 차단 |
| SEC-009 | 정상 Access Token | Gateway 통과 |
| SEC-010 | Bearer 없음 | 401 |
| SEC-011 | Bearer Prefix 오류 | 401 |
| SEC-012 | JWT 서명 위변조 | 401 |
| SEC-013 | Access 만료 | Refresh 유도 |
| SEC-014 | issuer 불일치 | 401 |
| SEC-015 | audience 불일치 | 401 |
| SEC-016 | userId Claim 없음 | 401 |
| SEC-017 | JWKS URI 누락 | 기동·503 오류 |
| SEC-018 | JWKS 일시 장애·Cache 있음 | 기존 키 검증 |
| SEC-019 | JWKS 장애·Cache 없음 | Fail Closed |
| SEC-020 | 알 수 없는 kid | 401·JWKS 갱신 |
| SEC-021 | JWT·Header 사용자 일치 | 정상 |
| SEC-022 | JWT·Header 사용자 불일치 | 차단 |
| SEC-023 | JWT·Header 지점 불일치 | 차단 |
| SEC-024 | JWT·Header 채널 불일치 | 정책상 차단 |
| SEC-025 | 기능권한 있음 | Handler 실행 |
| SEC-026 | 기능권한 없음 | 403·권한 오류 |
| SEC-027 | 데이터권한 있음 | 정상 |
| SEC-028 | 타 지점 데이터 | 권한 오류 |
| SEC-029 | 작성자 본인 승인 | 분리권한 오류 |
| SEC-030 | Refresh 정상 | Rotation·신규 Pair |
| SEC-031 | Refresh Hash 없음 | 재로그인 |
| SEC-032 | Refresh 만료 | 재로그인 |
| SEC-033 | Rotation Token 재사용 | Family 폐기 |
| SEC-034 | 사용자 잠금 후 Refresh | 발급 차단 |
| SEC-035 | 로그아웃 | Access·Refresh 폐기 |
| SEC-036 | 로그아웃 후 Access 재사용 | 거절 |
| SEC-037 | 특정 Family 강제종료 | 해당 장치만 종료 |
| SEC-038 | 사용자 전체 강제종료 | 모든 장치 종료 |
| SEC-039 | 권한 변경 | 기존 Token 정책 적용 |
| SEC-040 | Gateway 직접 경로 | 정상 인증 |
| SEC-041 | 업무 WAR 직접 접근·Token 없음 | 차단 |
| SEC-042 | 업무 WAR 직접 접근·정상 JWT | 정책상 허용·차단 |
| SEC-043 | Filter 비활성·직접 접근 | Gate 실패 |
| SEC-044 | 동일 Tomcat 다른 WAR | 같은 JWKS 검증 |
| SEC-045 | WAR별 독자 Private Key | Gate 실패 |
| SEC-046 | tcf-jwt 재기동 | 기존 Token 유지 확인 |
| SEC-047 | 키 회전 중 구 Token | 검증 성공 |
| SEC-048 | 구키 제거 후 만료 Token | 거절 |
| SEC-049 | Private Key 유출 훈련 | 전체 폐기·재로그인 |
| SEC-050 | JWT 원문 로그 | 보안 Gate 실패 |
| SEC-051 | Refresh 원문 DB | 보안 Gate 실패 |
| SEC-052 | localStorage Refresh | 보안 검토 실패 |
| SEC-053 | 인증 저장소 장애 | 거래등급별 Fail Closed |
| SEC-054 | 권한 Cache 장애 | 전체 허용 금지 |
| SEC-055 | 36,000명 로그인 | 용량 기준 통과 |
| SEC-056 | JWKS 동시 갱신 | 과도한 발급 서버 부하 없음 |
| SEC-057 | Clock Skew 경계 | 정책대로 처리 |
| SEC-058 | 로그인 실패 반복 | 잠금·보안 알림 |
| SEC-059 | 관리자 강제종료 | 감사로그 |
| SEC-060 | GUID·TraceId 추적 | Gateway부터 업무까지 연결 |

# 따라 하는 실무 절차

## 1단계. 인증 경계를 그린다

SSO

tcf-jwt

Gateway

업무 WAR

STF

업무 Rule

완료 증적:

구성도

Endpoint

Port

인증 책임표

## 2단계. JWT Claim을 확정한다

필수 Claim

선택 Claim

민감정보 제외

issuer

audience

수명

## 3단계. 키 관리방식을 확정한다

Private Key 저장소

JWKS

kid

Rotation

긴급 폐기

DR

## 4단계. SSO 발급 흐름을 검증한다

SSO 결과 검증

내부 호출 서명

Timestamp

Assertion 재사용

사용자 상태

## 5단계. Gateway 검증을 확인한다

Bearer

서명

exp

iss

aud

jti

Header 사용자

## 6단계. 직접 접근 방어를 확인한다

네트워크 ACL

업무 WAR Filter

required-for-online

인증 면제 목록

## 7단계. 권한을 분리한다

기능권한

데이터권한

상태권한

승인 분리

## 8단계. Refresh·로그아웃을 검증한다

Hash

Rotation

재사용

DenyList

Family 폐기

## 9단계. 만료·위변조·권한 테스트를 실행한다

401

403

Header mismatch

Refresh reuse

사용자 잠금

## 10단계. 로그와 감사를 확인한다

userId

jti

serviceId

failureReason

Token 원문 없음

강제종료 감사

## 11단계. 성능·장애 시험을 수행한다

36,000 사용자

JWKS 장애

인증 DB 장애

권한 Cache 장애

키 회전

## 12단계. 운영 Runbook을 완성한다

Token 탈취

Private Key 유출

SSO 장애

JWKS 장애

대량 인증 실패

권한 오설정

# 완료 체크리스트

## 인증

| 확인 항목 | 완료 |
| --- | --- |
| SSO와 JWT의 역할을 구분했다. | □ |
| SSO 결과 서명을 검증한다. | □ |
| Assertion 재사용을 차단한다. | □ |
| tcf-jwt만 Token을 발급한다. | □ |
| Access·Refresh를 구분한다. | □ |
| 필수 Claim을 정의했다. | □ |
| issuer·audience를 검증한다. | □ |
| Access 유효시간을 확정했다. | □ |
| Clock Skew를 확정했다. | □ |

## 키 관리

| 확인 항목 | 완료 |
| --- | --- |
| Private Key가 Git에 없다. | □ |
| Private Key가 업무 WAR에 없다. | □ |
| HSM·KMS·Vault를 사용한다. | □ |
| JWKS가 제공된다. | □ |
| kid가 존재한다. | □ |
| 다중 키 회전을 지원한다. | □ |
| 구키 유지기간이 있다. | □ |
| 긴급 폐기 Runbook이 있다. | □ |
| 재기동 후 기존 Token이 유지된다. | □ |

## Refresh·로그아웃

| 확인 항목 | 완료 |
| --- | --- |
| Refresh 원문을 저장하지 않는다. | □ |
| Refresh Rotation이 있다. | □ |
| 재사용 탐지가 있다. | □ |
| 재사용 시 Family를 폐기한다. | □ |
| Idle·Absolute Timeout이 있다. | □ |
| 특정 장치 종료가 가능하다. | □ |
| 전체 장치 종료가 가능하다. | □ |
| Access DenyList가 있다. | □ |
| Token Version 정책이 있다. | □ |

## Gateway·업무 WAR

| 확인 항목 | 완료 |
| --- | --- |
| Gateway가 JWT를 검증한다. | □ |
| JWT Claim과 Header를 비교한다. | □ |
| 직접 접근이 네트워크로 차단된다. | □ |
| 직접 접근 가능 시 Filter가 있다. | □ |
| 운영에서 Filter가 필수 설정이다. | □ |
| 인증 면제 목록이 최소화됐다. | □ |
| Filter 오류가 표준 응답이다. | □ |
| 인증정보가 요청 종료 시 clear된다. | □ |

## 권한

| 확인 항목 | 완료 |
| --- | --- |
| 인증과 인가를 구분했다. | □ |
| ServiceId 기능권한이 있다. | □ |
| 데이터권한 Rule이 있다. | □ |
| 상태별 권한이 있다. | □ |
| 권한정보가 없으면 차단한다. | □ |
| 작성자·승인자 분리가 있다. | □ |
| 권한 변경 Cache Evict가 있다. | □ |
| 기존 Token 반영정책이 있다. | □ |

## 보안·운영

| 확인 항목 | 완료 |
| --- | --- |
| JWT 원문을 로그에 남기지 않는다. | □ |
| Refresh 원문을 로그에 남기지 않는다. | □ |
| Claim에 개인정보가 없다. | □ |
| 인증 실패 Metric이 있다. | □ |
| Refresh 재사용 Alert가 있다. | □ |
| 관리자 강제종료 감사가 있다. | □ |
| JWKS 장애 정책이 있다. | □ |
| 인증 저장소 장애정책이 있다. | □ |
| 키 유출 사고훈련을 수행했다. | □ |
| 36,000명 용량시험을 수행했다. | □ |

# 변경·호환성·폐기 관리

## Claim 추가

선택 Claim 추가는 비교적 호환 가능하다.

그러나 Token 크기와 개인정보를 검토해야 한다.

## Claim 삭제

branchId 삭제

영향:

Gateway Header 보정

STF 정합성

업무 데이터권한

로그

Client

비호환 변경일 수 있다.

## Claim 의미 변경

authGroupId
기존 = OM 권한그룹
신규 = 인사 직급

같은 Claim 이름으로 의미를 변경하지 않는다.

신규 Claim 또는 Token Version을 사용한다.

## Issuer 변경

NSIGHT-AUTH
→ NH-AUTH-V2

Gateway·업무 WAR 설정과 병행 검증기간이 필요하다.

## Audience 분리

기존 단일 Audience에서 OM·업무를 분리하면 기존 Token의 호출 가능 범위가 달라진다.

병행 발급·소비자 전환 계획이 필요하다.

## Algorithm 변경

RS256
→ ES256

영향:

키 저장

JWKS

Gateway Decoder

HSM 지원

성능

구 Token

다중 알고리즘 병행 기간과 폐기계획이 필요하다.

## Access Token 유효시간 변경

단축:

보안 향상

Refresh 호출 증가

사용자 인증 장애 영향 증가

연장:

사용자 편의

탈취·권한변경 반영 위험 증가

성능과 보안을 함께 시험한다.

## Session에서 JWT로 전환

1\. HttpSession 사용처 식별
2\. JWT 병행 적용
3\. 인증방식별 Metric 분리
4\. Token Family 도입
5\. Session 강제종료를 Token 폐기로 전환
6\. AuthenticationContextValidator 필수화
7\. Session 신규 생성 중지
8\. 잔여 Session 만료
9\. Session 인프라 제거

## 키 폐기

신규 서명 중지

구키 JWKS 유지

구 Token 만료 확인

호출량 확인

공개키 제거

Private Key 파기 증적

침해사고가 아니라 정상 Rotation이라면 순차 폐기한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 인증과 인가는 서로 다른 책임이다.

인증
\= 누구인가

인가
\= 무엇을 할 수 있는가

둘째, JWT는 사용자 권한 전체를 보장하지 않는다.

JWT
→ 신원과 최소 속성

STF
→ 기능 실행권한

업무 Rule
→ 데이터·상태 권한

셋째, 완전 Stateless JWT는 강제 로그아웃·권한 변경·탈취 대응에 한계가 있으므로 Access Token은 Stateless하게 검증하되 Refresh·Family·DenyList·Token Version의 최소 상태를 유지하는 것이 적절하다.

넷째, Gateway 인증이 존재해도 직접 접근 가능성이 있다면 업무 WAR에서 JWT를 다시 검증해야 한다.

다섯째, 같은 Tomcat의 여러 WAR는 HttpSession을 자동 공유하지 않지만 동일 발급자의 JWKS 공개키로 JWT를 각각 검증할 수 있다.

여섯째, Private Key는 사용자별·WAR별 키가 아니며 발급 서비스의 핵심 보안자산이다.

일곱째, 기동할 때마다 새로운 서명키를 생성하는 구현은 로컬 개발에는 사용할 수 있지만 운영에는 사용할 수 없다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 인증과 인가 혼동 | 과도한 권한 |
| Header 사용자 신뢰 | 사용자 위변조 |
| Gateway만 검증 | 우회 접근 |
| JWT Claim 과다 | 개인정보 노출 |
| 완전 Stateless | 강제 폐기 불가 |
| Refresh 원문 저장 | 인증 탈취 |
| Rotation 재사용 미탐지 | 장기 계정 탈취 |
| Private Key 소스 저장 | 전체 Token 위조 |
| 기동 시 키 생성 | 재기동 후 전 사용자 장애 |
| 고정 kid | 안전한 키 회전 불가 |
| DenyList 미연계 | 로그아웃 Token 재사용 |
| 권한 Cache 무효화 없음 | 회수 권한 계속 사용 |
| 업무별 JWT 구현 | 보안정책 불일치 |
| 인증 실패 전체 허용 | Fail Open |
| JWT 원문 로그 | Token 유출 |
| 단순 Filter JSON | 오류 계약 불일치 |
| Session 대형 객체 | DB·Heap 부하 |
| JWKS 장애 무정책 | 전체 인증 장애 |

## 우선 보완 과제

1.  기동 시 RSA Key 생성방식을 운영용 HSM·KMS·Vault 키로 교체한다.
2.  다중 kid와 JWKS 기반 키 회전을 구현한다.
3.  Gateway와 업무 WAR에서 DenyList·Token Version 검증을 연계한다.
4.  Refresh 재사용 시 Token Family 전체 폐기를 구현한다.
5.  운영에서 Header 사용자 Strict 검증을 활성화한다.
6.  Gateway 우회 네트워크 ACL과 업무 WAR Filter를 검증한다.
7.  JWT Filter 오류 응답을 StandardResponse로 통일한다.
8.  JWT 관련 System.out을 구조화 보안로그로 교체한다.
9.  Access·Refresh Token 원문 로그를 정적검사로 차단한다.
10.  기능권한과 데이터권한의 책임을 공식화한다.
11.  권한 변경 시 Cache Evict·Token Version 정책을 구현한다.
12.  인증 저장소 장애의 거래등급별 Fail Closed 정책을 정의한다.
13.  JWKS Cache·키 회전·장애 시험을 자동화한다.
14.  Token Family·활성 로그인·강제 종료 OM 화면을 구현한다.
15.  36,000명 로그인·Refresh·DenyList 용량시험을 수행한다.

## 중장기 발전 방향

개발용 기동 키
↓
Vault 영속 키

단일 고정 kid
↓
다중 kid Rotation

Access·Refresh 기본구조
↓
Token Family·재사용 탐지

Gateway 단일 검증
↓
Gateway 우회 방어·STF 정합성

기능권한 중심
↓
기능·데이터·상태 권한

수동 Token 운영
↓
OM 활성 로그인·강제폐기·감사

개별 보안 테스트
↓
CI/CD 보안 Gate·침해사고 훈련

# 마무리말

인증·인가·세션·JWT를 설계하는 과정은 다음 질문에 답하는 일이다.

사용자의 최초 신원은 누가 확인하는가?

SSO 결과를 어떻게 검증하는가?

JWT는 어느 서비스가 발급하는가?

Private Key는 어디에 보관하는가?

Gateway는 어떤 Claim을 검증하는가?

업무 WAR 직접 접근은 차단됐는가?

JWT와 Header의 사용자는 일치하는가?

사용자가 해당 ServiceId를 실행할 수 있는가?

해당 데이터에 접근할 수 있는가?

현재 상태에서 그 행위를 할 수 있는가?

Access Token과 Refresh Token의 수명은 얼마인가?

Refresh Token은 Hash로 저장되는가?

Rotation된 Token이 재사용되면 어떻게 하는가?

로그아웃과 강제 로그아웃은 무엇을 폐기하는가?

권한이 변경되면 기존 Token은 언제 무효화되는가?

JWKS와 인증 저장소가 장애 나면 어떻게 하는가?

키가 유출되면 어떤 순서로 모든 Token을 폐기하는가?

Token 원문 없이도 로그와 감사로 사고를 추적할 수 있는가?

제22장의 핵심 흐름은 다음과 같다.

SSO 신원 확인
↓
tcf-jwt Token 발급
↓
Gateway·WAR 서명 검증
↓
AuthenticationContext
↓
STF Claim·Header 정합성
↓
기능권한
↓
업무 데이터·상태 권한
↓
거래 실행
↓
로그·감사·폐기 관리

가장 중요한 원칙은 다음과 같다.

JWT가 있다고
사용자를 신뢰하는 것이 아니다.

JWT의 서명이 맞다고
업무를 허용하는 것도 아니다.

신원 검증,
기능권한,
데이터권한,
Token 상태와 키 관리가

각자의 경계에서 함께 작동해야
신뢰할 수 있는 인증 구조가 된다.
