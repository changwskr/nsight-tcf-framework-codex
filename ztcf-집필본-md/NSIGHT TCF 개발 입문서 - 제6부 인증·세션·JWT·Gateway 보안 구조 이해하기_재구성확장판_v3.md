<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제6부 인증·세션·JWT·Gateway 보안 구조 이해하기_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제6부. 인증·세션·JWT·Gateway 보안 구조 이해하기의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

인증·인가, 시스템 간 계약, 캐시·배치 등 분산 환경의 경계를 이해합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

3단계 · 보안과 연계

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제6부. 인증·세션·JWT·Gateway 보안 구조 이해하기**

# **1\. 도입 전 안내말**

제5부에서는 오류, Timeout, 거래통제, 중복방지와 거래 추적을 배웠습니다.

제6부에서는 사용자가 로그인한 뒤 업무 거래를 실행할 때 다음 질문에 어떻게 답하는지 살펴봅니다.

이 요청을 보낸 사용자는 누구인가?

정말 로그인한 사용자가 맞는가?

이 사용자가 이 ServiceId를 실행할 권한이 있는가?

다른 사용자의 정보를 조회하려는 것은 아닌가?

토큰이 만료되거나 위조되지는 않았는가?

Gateway를 우회해 업무 WAR로 직접 접근하면 어떻게 막는가?

Tomcat에 여러 업무 WAR가 배포되면
각 WAR는 토큰을 어떻게 검증하는가?

화면에서 다음 값을 보냈다고 가정해 봅시다.

{
"header": {
"userId": "admin",
"branchId": "999999",
"serviceId": "SV.Customer.selectSummary"
}
}

서버가 이 값을 그대로 믿으면 안 됩니다.

사용자는 개발자 도구나 별도의 프로그램을 이용해 요청 값을 바꿀 수 있기 때문입니다.

화면이 보낸 userId
\= 주장한 사용자 정보

JWT 검증 후 만들어진 AuthenticationContext
\= 서버가 검증한 사용자 정보

서버는 화면이 주장한 사용자가 아니라, 인증 시스템이 검증한 사용자를 기준으로 거래를 처리해야 합니다.

이번 부의 핵심 원칙은 다음과 같습니다.

인증되지 않은 요청은 업무 로직에 도달하지 않는다.

사용자 ID와 지점정보는 요청 Body보다
검증된 인증 Context를 우선한다.

Gateway가 있더라도 업무 WAR는
최소한의 2차 방어를 수행한다.

Gateway가 없다면 각 업무 WAR가
직접 토큰을 검증해야 한다.

Private Key는 토큰 발급 서버만 가진다.

업무 WAR는 Public Key로 서명만 검증한다.

# **2\. 제6부 개요**

## **2.1 목적**

제6부의 목적은 초보 개발자가 인증·인가·세션·JWT의 차이를 이해하고, NSIGHT TCF 거래에서 사용자 신원과 권한을 안전하게 전달하고 검증하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 내용을 설명할 수 있어야 합니다.

1.  인증과 인가의 차이를 설명한다.
2.  세션과 JWT의 차이를 설명한다.
3.  SSO 로그인부터 JWT 발급까지의 흐름을 이해한다.
4.  Access Token과 Refresh Token의 역할을 구분한다.
5.  JWT Claim의 의미와 검증항목을 이해한다.
6.  RS256의 Private Key와 Public Key 역할을 구분한다.
7.  Gateway에서 JWT를 검증하는 흐름을 이해한다.
8.  Gateway가 없을 때 업무 WAR의 검증책임을 설명한다.
9.  Tomcat 다중 WAR 환경의 Public Key 관리방식을 이해한다.
10.  요청 Header와 AuthenticationContext의 정합성을 검사한다.
11.  기능권한과 데이터권한을 구분한다.
12.  인증·권한 오류와 보안감사로그를 설계한다.

## **2.2 적용범위**

| **영역** | **주요 내용** |
| --- | --- |
| 로그인 | SSO 인증, 사용자 확인 |
| 세션 | HttpSession, OM 관리 세션 |
| JWT | Access Token, Refresh Token |
| 서명 | RS256, Private Key, Public Key |
| Gateway | 토큰 검증, Route, 인증 Context 전달 |
| 업무 WAR | 2차 검증, 직접 접근 방어 |
| TCF | STF 인증·권한 검증 |
| 권한 | 기능권한, 데이터권한 |
| Context | 사용자, 지점, 채널, 역할 |
| 보안로그 | 인증·권한·토큰 오류 |
| 운영 | 키 교체, 토큰 만료, 장애 대응 |
| 테스트 | 정상·위조·만료·우회 접근 |

## **2.3 대상 독자**

-   로그인과 권한의 차이가 혼동되는 개발자
-   세션과 JWT 중 무엇을 써야 하는지 궁금한 개발자
-   JWT가 단순 암호화 문자열이라고 생각하는 개발자
-   Gateway가 토큰을 검증하면 업무 서버는 아무것도 하지 않아도 된다고 생각하는 개발자
-   여러 WAR가 같은 Public Key를 사용해도 되는지 궁금한 개발자
-   SSO 로그인 이후 업무 요청 흐름을 처음 구현하는 개발자
-   인증·권한 테스트를 작성해야 하는 개발자

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

화면은 ServiceId가 포함된 표준 요청을 보낸다.

Gateway는 외부 요청을 적절한 업무 WAR로 전달한다.

STF는 Handler 실행 전에 공통 검증을 수행한다.

TransactionContext는 업무 처리에 필요한 공통 문맥이다.

Handler 이하 업무 계층은 검증된 Context를 사용한다.

## **2.5 주요 용어**

| **용어** | **쉬운 설명** |
| --- | --- |
| 인증 | 사용자가 누구인지 확인하는 것 |
| 인가 | 인증된 사용자가 무엇을 할 수 있는지 확인하는 것 |
| SSO | 한 번 로그인하여 여러 시스템을 사용하는 방식 |
| IdP | 사용자를 인증하는 인증 제공자 |
| 세션 | 서버가 로그인 상태를 기억하는 방식 |
| JWT | 사용자와 권한정보를 담고 서명한 토큰 |
| Access Token | 업무 API 호출에 사용하는 짧은 수명의 토큰 |
| Refresh Token | Access Token을 다시 발급받기 위한 토큰 |
| Claim | JWT 안에 들어 있는 정보 항목 |
| Signature | 토큰이 위조되지 않았음을 검증하는 서명 |
| RS256 | RSA Private Key로 서명하고 Public Key로 검증하는 방식 |
| JWKS | Public Key를 표준 JSON 형식으로 제공하는 정보 |
| AuthenticationContext | 검증된 사용자 인증정보 |
| Authorization | 사용자의 기능·데이터 접근권한 판단 |
| Principal | 인증된 사용자 주체 |
| Issuer | 토큰을 발급한 시스템 |
| Audience | 토큰을 사용할 수 있는 대상 시스템 |
| Denylist | 사용을 중지시킨 토큰 목록 |
| Key Rotation | 서명 키를 안전하게 교체하는 절차 |

# **제37장. 인증·인가·세션·JWT 구분하기**

학습 목표 | 37장. 인증·인가·세션·JWT 구분하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **37.1 인증이란 무엇인가요?**

인증은 사용자가 누구인지 확인하는 과정입니다.

사용자
“나는 user01입니다.”

인증 서버
“비밀번호·인증서·SSO 결과를 확인했습니다.
실제로 user01이 맞습니다.”

인증 수단의 예:

-   아이디와 비밀번호
-   인증서
-   생체인증
-   일회용 비밀번호
-   SSO
-   사내 인증 토큰

인증이 완료되면 서버는 사용자를 식별할 수 있는 정보를 만듭니다.

userId
employeeNo
branchId
organizationCode
roles
authenticationTime

## **37.2 인가란 무엇인가요?**

인가는 인증된 사용자가 특정 기능을 실행할 수 있는지 판단하는 과정입니다.

user01이 로그인했다.
→ 인증 성공

user01이 캠페인 승인 권한을 가지고 있는가?
→ 인가 판단

예:

| **사용자** | **기능** | **결과** |
| --- | --- | --- |
| 일반 조회 사용자 | 고객조회 | 허용 |
| 일반 조회 사용자 | 캠페인 승인 | 거부 |
| 캠페인 관리자 | 캠페인 승인 | 허용 |
| 다른 지점 사용자 | 타 지점 고객조회 | 데이터권한에 따라 거부 |

## **37.3 인증과 인가를 분리해야 하는 이유**

로그인했다고 모든 기능을 사용할 수 있는 것은 아닙니다.

인증
\= 회사 직원임을 확인

인가
\= 해당 직원이 고객정보를 조회할 수 있는지 확인

금지 구조:

if (userId != null) {
// 모든 기능 허용
}

권장 구조:

인증 성공
→ 사용자 Context 생성
→ ServiceId 기능권한 확인
→ 데이터 범위 권한 확인

## **37.4 세션이란 무엇인가요?**

세션은 서버가 사용자의 로그인 상태를 기억하는 방식입니다.

로그인 시:

사용자 로그인
→ 서버가 Session 생성
→ Session ID를 Cookie로 전달

이후 요청:

화면이 Session ID 전달
→ 서버가 Session 조회
→ 사용자정보 확인

예:

JSESSIONID=ABC123

서버 세션 내부에는 다음 정보가 있을 수 있습니다.

userId=user01
branchId=001234
roles=\[SV\_VIEWER\]
loginDtm=...

## **37.5 JWT란 무엇인가요?**

JWT는 서버가 사용자정보를 담아 서명한 문자열입니다.

형태:

Header.Payload.Signature

예:

eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleS0wMSJ9
.
eyJzdWIiOiJ1c2VyMDEiLCJicmFuY2hJZCI6IjAwMTIzNCJ9
.
서명값

JWT는 일반적으로 암호화된 데이터가 아니라 인코딩된 데이터입니다.

따라서 Payload는 쉽게 읽을 수 있습니다.

JWT에 비밀번호와 민감정보를 넣으면 안 된다.

서명의 목적은 내용을 숨기는 것이 아니라 변경 여부를 확인하는 것입니다.

## **37.6 세션과 JWT 비교**

| **구분** | **세션** | **JWT** |
| --- | --- | --- |
| 상태 저장 | 서버에 저장 | 토큰에 정보 포함 |
| 사용자 식별 | Session ID 조회 | 토큰 검증 |
| 서버 확장 | 공유 Session 필요 | 비교적 용이 |
| 즉시 로그아웃 | Session 삭제 | 별도 전략 필요 |
| 토큰 크기 | Cookie가 작음 | JWT가 상대적으로 큼 |
| 권한 변경 반영 | Session 갱신 | 재발급 전까지 지연 가능 |
| 서버 부하 | Session 저장소 사용 | 서명 검증 사용 |
| 탈취 위험 | Session ID 탈취 | Access Token 탈취 |
| 주요 보호 | Cookie 보안 | 토큰 저장·만료·서명 |

## **37.7 세션을 없애면 모든 문제가 해결될까요?**

그렇지 않습니다.

세션을 제거하고 JWT를 사용하면 서버 간 Session 공유 부담은 줄어들 수 있습니다.

하지만 다음 문제가 새로 중요해집니다.

토큰 탈취
토큰 만료
Refresh Token 관리
강제 로그아웃
권한 변경 반영
키 교체
Token 재사용
클라이언트 저장 위치

따라서 다음처럼 판단하면 안 됩니다.

JWT 사용
\= 보안 문제 해결

정확한 판단:

JWT 사용
\= 서버 상태 의존을 줄이는 인증 전달방식

보안
\= 토큰 수명·저장·검증·키 관리·권한 정책을 함께 설계

## **37.8 하이브리드 방식**

NSIGHT에서는 사용자 화면이나 OM 관리화면 특성에 따라 세션과 JWT를 함께 사용할 수 있습니다.

예:

OM 관리화면
→ HttpSession으로 관리자 화면 상태 유지

업무 API
→ JWT Access Token으로 인증

Refresh Token
→ 안전한 저장소 또는 서버측 관리

이 방식은 다음처럼 역할을 나눕니다.

세션
\= 화면 로그인 상태와 관리 UI 편의

JWT
\= Gateway·업무 WAR 간 인증정보 전달

## **제37장 요약**

학습 목표 | 37장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

인증
\= 사용자가 누구인지 확인

인가
\= 무엇을 할 수 있는지 확인

세션
\= 서버가 로그인 상태를 기억

JWT
\= 서명된 인증정보를 요청과 함께 전달

세션과 JWT는
서로 완전히 반대되는 기술이 아니라
필요에 따라 함께 사용할 수 있다.

# **제38장. SSO 로그인과 토큰 발급 흐름**

학습 목표 | 38장. SSO 로그인과 토큰 발급 흐름의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **38.1 SSO 로그인 전체 흐름**

NSIGHT의 개념적인 SSO 로그인 흐름은 다음과 같습니다.

\[사용자\]
│
│ 1. SSO 로그인 클릭
▼
\[화면 / 전용 브라우저\]
│
│ 2. SSO 인증 요청
▼
\[SSO 서버 / IdP\]
│
│ 3. 사용자 인증
│ 4. 인증 Code·Token·Assertion 발급
▼
\[화면\]
│
│ 5. OM.Auth.ssoLogin 요청
▼
\[tcf-om\]
│
├─ 6. SSO 결과 검증
├─ 7. OM\_USER 조회
├─ 8. 사용자 상태·권한 확인
├─ 9. 필요 시 HttpSession 생성
└─ 10. tcf-jwt에 JWT 발급 요청
│
▼
\[tcf-jwt\]
Access Token
Refresh Token 발급
│
▼
\[tcf-om\]
│
│ 11. Session·Token 응답
▼
\[화면\]
│
│ 12. 이후 업무 API 호출
▼
\[Gateway 또는 업무 WAR\]

## **38.2 SSO가 대신해 주는 것**

SSO는 사용자의 최초 인증을 담당합니다.

사용자 ID
인증 성공 여부
인증 시각
인증 방식
조직정보 일부

하지만 SSO 인증만 성공했다고 NSIGHT의 모든 기능을 허용해서는 안 됩니다.

NSIGHT는 추가로 확인해야 합니다.

NSIGHT 사용자로 등록되어 있는가?

퇴직·잠금·사용중지 상태가 아닌가?

어느 업무권한을 가지는가?

어느 지점·조직에 속하는가?

관리자 기능 사용이 가능한가?

## **38.3 SSO 결과 검증**

SSO 서버에서 전달받은 Token이나 Assertion을 단순 문자열로 믿으면 안 됩니다.

검증항목:

서명 유효성
발급자
대상 시스템
만료시간
발급시간
Nonce·State
재사용 여부
사용자 식별자

금지:

String userId = request.getParameter("userId");
login(userId);

권장:

SSO가 서명한 인증결과 검증
→ 검증된 사용자 식별자 추출
→ 내부 사용자정보 조회

## **38.4 내부 사용자 조회**

SSO가 사용자 user01을 인증했다고 가정합니다.

tcf-om은 내부 사용자 기준정보를 조회합니다.

| **항목** | **예** |
| --- | --- |
| 사용자 ID | user01 |
| 사용자 상태 | ACTIVE |
| 지점 ID | 001234 |
| 조직코드 | ORG001 |
| 역할 | SV\_VIEWER |
| 기능권한 | SV\_CUSTOMER\_VIEW |
| 관리자 여부 | N |
| 최종 권한변경시각 | 2026-07-17 |

SSO에는 사용자가 존재하지만 내부 사용자 기준정보가 없으면 로그인을 차단하거나 별도 등록절차를 적용합니다.

## **38.5 계정 상태 확인**

ACTIVE
→ 로그인 허용

LOCKED
→ 로그인 차단

SUSPENDED
→ 로그인 차단

EXPIRED
→ 정책에 따라 갱신 안내

DELETED
→ 로그인 차단

사용자 상태는 JWT 발급 전에 확인해야 합니다.

## **38.6 HttpSession 생성**

OM 화면이 서버 세션을 사용하는 경우 로그인 성공 후 Session을 생성할 수 있습니다.

HttpSession session =
request.getSession(true);

session.setAttribute(
"AUTH\_USER",
authenticatedUser
);

Session에는 필요한 최소 정보만 저장합니다.

금지:

대용량 사용자 전체 객체
비밀번호
SSO 원본 Token
Private Key
불필요한 개인정보

## **38.7 JWT 발급 요청**

tcf-om은 검증된 내부 사용자정보를 이용해 tcf-jwt에 토큰 발급을 요청합니다.

개념적인 ServiceId:

JWT.Auth.ssoIssue

요청 데이터:

{
"userId": "user01",
"branchId": "001234",
"organizationCode": "ORG001",
"roles": \[
"SV\_VIEWER"
\],
"authorities": \[
"SV\_CUSTOMER\_VIEW"
\],
"authenticationType": "SSO"
}

이 요청은 외부 화면이 직접 만들지 않습니다.

검증된 서버 구성요소가 생성해야 합니다.

## **38.8 로그인 응답**

개념 예:

{
"result": {
"resultStatus": "SUCCESS",
"resultCode": "S0000"
},
"body": {
"accessToken": "eyJ...",
"expiresIn": 900,
"tokenType": "Bearer",
"user": {
"userId": "user01",
"branchId": "001234",
"displayName": "홍길동"
}
}
}

Refresh Token은 보안정책에 따라 다음 방식 중 하나로 전달할 수 있습니다.

-   HttpOnly Secure Cookie
-   서버측 저장
-   암호화 저장소
-   별도 토큰 교환 흐름

일반 JavaScript가 읽을 수 있는 Local Storage에 장기간 Refresh Token을 저장하는 방식은 신중하게 검토해야 합니다.

## **38.9 로그인 실패 흐름**

### **SSO 검증 실패**

SSO Token 위조·만료
→ 내부 사용자 조회 안 함
→ JWT 발급 안 함
→ 인증 오류

### **내부 사용자 없음**

SSO 인증 성공
→ OM\_USER 없음
→ 접근 차단 또는 사용자 등록절차

### **계정 잠금**

SSO 인증 성공
→ OM\_USER 상태 LOCKED
→ JWT 발급 안 함
→ 보안로그 기록

### **JWT 발급 실패**

SSO와 사용자 검증 성공
→ tcf-jwt 장애
→ 로그인 실패
→ Session 생성 여부 Rollback·정리

## **제38장 요약**

학습 목표 | 38장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

SSO는 사용자의 최초 신원을 확인한다.

tcf-om은 내부 사용자 상태와 권한을 확인한다.

tcf-jwt는 검증된 사용자정보를 이용해
Access Token과 Refresh Token을 발급한다.

화면이 주장한 사용자정보로
JWT를 발급해서는 안 된다.

# **제39장. JWT 구조와 Claim 설계**

학습 목표 | 39장. JWT 구조와 Claim 설계의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **39.1 JWT의 세 부분**

JWT는 다음 세 부분으로 구성됩니다.

Header.Payload.Signature

### **Header**

서명 알고리즘과 Key ID를 포함합니다.

{
"alg": "RS256",
"typ": "JWT",
"kid": "nsight-key-2026-01"
}

### **Payload**

사용자와 토큰 정보를 Claim으로 담습니다.

{
"iss": "nsight-auth",
"sub": "user01",
"aud": \[
"nsight-gateway",
"nsight-services"
\],
"iat": 1784239200,
"exp": 1784240100,
"jti": "token-uuid",
"branchId": "001234",
"roles": \[
"SV\_VIEWER"
\]
}

### **Signature**

Header와 Payload가 변경되지 않았음을 검증합니다.

## **39.2 표준 Claim**

| **Claim** | **의미** |
| --- | --- |
| iss | 토큰 발급자 |
| sub | 토큰 주체, 일반적으로 사용자 ID |
| aud | 토큰 사용 대상 |
| iat | 발급시각 |
| exp | 만료시각 |
| nbf | 사용 가능 시작시각 |
| jti | 토큰 고유 ID |

이 항목들은 토큰 검증의 핵심입니다.

## **39.3 업무용 Claim**

NSIGHT에서 검토할 수 있는 Claim:

| **Claim** | **예** | **용도** |
| --- | --- | --- |
| branchId | 001234 | 사용자 지점 |
| organizationCode | ORG001 | 조직 |
| roles | SV\_VIEWER | 역할 |
| authType | SSO | 인증방식 |
| sessionId | 선택 | Session 연결 |
| tokenVersion | 1 | 강제 무효화 지원 |
| channelId | 선택 | 발급 채널 |

JWT에 모든 기능권한을 넣으면 토큰 크기가 커지고 권한 변경 반영이 늦어질 수 있습니다.

따라서 다음 중 하나를 선택합니다.

역할 중심 Claim
→ 서버가 역할별 권한 조회

핵심 권한만 Claim
→ 세부권한은 서버 조회

전체 권한 Claim
→ 짧은 Token 수명과 변경정책 필요

## **39.4 JWT에 넣으면 안 되는 정보**

비밀번호
주민등록번호
계좌번호
Private Key
Refresh Token
DB 권한정보
상세 개인정보
변경이 잦은 대용량 메뉴 목록

JWT Payload는 암호화되지 않을 수 있으므로 민감정보 저장소로 사용하면 안 됩니다.

## **39.5 Access Token**

Access Token은 업무 API 호출에 사용합니다.

특성:

수명이 짧다.
매 요청에 전달한다.
Gateway 또는 업무 WAR가 검증한다.
탈취되면 만료 전까지 악용될 수 있다.

예:

유효시간 15분

실제 시간은 보안정책과 사용자 편의성을 고려해 확정합니다.

## **39.6 Refresh Token**

Refresh Token은 만료된 Access Token을 다시 발급받는 데 사용합니다.

특성:

Access Token보다 수명이 길다.
업무 API 호출에 직접 사용하지 않는다.
더 안전하게 저장해야 한다.
재사용 탐지가 필요할 수 있다.

금지:

Authorization: Bearer {refreshToken}
→ 일반 업무 ServiceId 호출

## **39.7 토큰 갱신 흐름**

Access Token 만료
→ 화면이 Refresh 요청
→ Refresh Token 검증
→ 사용자 상태 확인
→ Token 재사용 여부 확인
→ 새 Access Token 발급
→ 필요 시 Refresh Token 교체

개념 ServiceId:

JWT.Auth.refresh

## **39.8 Refresh Token Rotation**

Refresh 요청마다 새 Refresh Token을 발급하는 방식입니다.

Refresh Token A 사용
→ Access Token B 발급
→ Refresh Token B 발급
→ A는 폐기

폐기된 A가 다시 사용되면 탈취 가능성을 탐지할 수 있습니다.

## **39.9 만료시간 검증**

현재 시각이 exp 이후이면 토큰을 거부합니다.

현재시각 > exp
→ TOKEN\_EXPIRED

서버 간 시각 차이를 고려해 작은 Clock Skew를 허용할 수 있습니다.

예:

허용 시각오차 30초

너무 큰 오차를 허용하면 만료 토큰이 오래 사용될 수 있습니다.

## **39.10 Issuer 검증**

iss = nsight-auth

검증 서버는 허용된 발급자인지 확인합니다.

서명만 유효하다고 모든 발급자의 토큰을 받아서는 안 됩니다.

## **39.11 Audience 검증**

토큰이 NSIGHT 업무용으로 발급되었는지 확인합니다.

aud includes nsight-services

다른 시스템용 토큰을 NSIGHT에서 사용하지 못하도록 합니다.

## **39.12 JTI와 토큰 추적**

jti는 토큰을 고유하게 식별합니다.

용도:

-   토큰 재사용 탐지
-   Refresh Token 상태관리
-   보안사고 추적
-   강제 무효화
-   감사로그 연결

단, 모든 Access Token을 DB에서 조회하면 Stateless 장점이 줄어듭니다.

중요 토큰에 한해 제한적인 Denylist나 Token Version 전략을 적용할 수 있습니다.

## **39.13 JWT 검증항목**

형식
서명
지원 알고리즘
kid
Public Key
issuer
audience
expiration
not-before
token type
jti
사용자 상태
필요 시 token version

alg=none이나 허용되지 않은 알고리즘은 거부해야 합니다.

## **제39장 요약**

학습 목표 | 39장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

JWT는 암호화 문서가 아니라
서명된 Claim 묶음이다.

Access Token은 짧게 사용하고,
Refresh Token은 더 안전하게 관리한다.

서명뿐 아니라
issuer·audience·만료시간도 검증한다.

# **제40장. RS256과 키 관리**

학습 목표 | 40장. RS256과 키 관리의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **40.1 대칭키와 비대칭키**

### **대칭키 방식**

같은 Secret으로 서명하고 검증합니다.

발급 서버
Secret 보유

검증 서버
같은 Secret 보유

검증 서버가 많아질수록 Secret이 여러 곳에 배포됩니다.

검증 서버가 침해되면 토큰을 새로 발급할 수도 있습니다.

### **비대칭키 방식**

Private Key
→ 서명

Public Key
→ 검증

검증 서버는 Public Key만 가지므로 토큰을 임의로 발급할 수 없습니다.

## **40.2 RS256 역할**

tcf-jwt
Private Key 보유
→ JWT 서명

Gateway·업무 WAR
Public Key 보유
→ JWT 검증

핵심 원칙:

Private Key는 발급 서버 밖으로 배포하지 않는다.

Public Key는 검증 서버에 안전하게 배포할 수 있다.

## **40.3 Private Key는 사용자별인가요?**

아닙니다.

일반적으로 Private Key는 토큰 발급 시스템의 서명키입니다.

사용자 A 토큰
사용자 B 토큰
사용자 C 토큰

→ 동일한 활성 Private Key로 서명 가능

사용자마다 Private Key를 만들 필요는 없습니다.

사용자를 구분하는 것은 JWT의 sub, jti, Claim입니다.

## **40.4 여러 업무 WAR의 Public Key**

Tomcat에 여러 WAR가 배포된 경우를 생각해 봅시다.

Tomcat
├─ sv-service.war
├─ ic-service.war
├─ cm-service.war
└─ om-service.war

모든 WAR가 동일한 인증 발급자를 신뢰한다면 같은 Public Key 집합을 사용할 수 있습니다.

tcf-jwt Private Key
│
└─ Public Key
├─ sv-service
├─ ic-service
├─ cm-service
└─ Gateway

업무 WAR별로 별도 Private Key를 생성할 필요는 없습니다.

## **40.5 WAR별 Public Key를 분리하는 경우**

다음과 같은 요구가 있을 때는 Audience 또는 별도 Key Domain을 검토할 수 있습니다.

외부 공개 API와 내부 업무 API 분리

업무군별 보안등급 차이

서로 다른 인증 발급자

규제상 키 영역 분리

관리자 토큰과 일반 사용자 토큰 분리

단순히 WAR가 여러 개라는 이유만으로 Public Key를 모두 다르게 관리하면 운영 복잡도가 크게 증가합니다.

## **40.6 JWKS**

JWKS는 Public Key 정보를 표준 JSON으로 제공합니다.

개념 예:

{
"keys": \[
{
"kty": "RSA",
"kid": "nsight-key-2026-01",
"use": "sig",
"alg": "RS256",
"n": "...",
"e": "AQAB"
}
\]
}

JWT Header의 kid와 일치하는 Key를 선택합니다.

JWT Header kid
→ JWKS에서 Public Key 조회
→ 서명 검증

## **40.7 키 교체**

키는 영구적으로 하나만 사용하지 않습니다.

교체 흐름:

1\. 신규 Key Pair 생성

2\. 신규 Public Key를 JWKS에 추가

3\. 기존·신규 Public Key를 함께 제공

4\. 신규 Private Key로 토큰 발급 시작

5\. 기존 토큰 만료시간까지 기존 Public Key 유지

6\. 기존 Key 제거

7\. 폐기 이력과 증적 보관

## **40.8 kid의 역할**

키 교체 기간에는 여러 Public Key가 동시에 존재할 수 있습니다.

kid=key-2026-01
kid=key-2026-02

검증 서버는 토큰 Header의 kid로 올바른 Public Key를 선택합니다.

kid가 없거나 등록되지 않은 값이면 토큰을 거부합니다.

## **40.9 Private Key 저장**

금지:

jwt:
private-key: |
\-----BEGIN PRIVATE KEY-----
...

소스 저장소와 일반 설정파일에 Private Key를 넣으면 안 됩니다.

권장 저장방식:

-   HSM
-   KMS
-   Secret Manager
-   OS 보호 저장소
-   접근통제된 암호화 파일
-   전용 인증 서버의 보호 영역

## **40.10 Public Key Cache**

업무 WAR가 매 요청마다 JWKS 서버를 호출하면 안 됩니다.

권장:

JWKS 조회
→ Public Key Cache
→ JWT 검증

필요 정책:

Cache TTL
kid 미발견 시 즉시 갱신
마지막 정상 Key 유지
JWKS 장애 경보
Key 만료 관리

## **40.11 JWKS 장애 시 처리**

JWKS 서버가 일시적으로 장애여도 이미 Cache된 유효 Public Key가 있다면 검증을 계속할 수 있습니다.

Cache된 Key 존재
→ 유효기간 내 검증 계속

새로운 kid
→ JWKS 갱신 실패
→ 해당 토큰 거부

새로운 Key를 검증할 수 없는데 무조건 허용하면 안 됩니다.

## **40.12 키 유출 대응**

Private Key 유출이 의심되면 다음 절차가 필요합니다.

영향 Key 식별
→ 신규 Key 발급
→ 발급 Key 전환
→ 기존 Key 폐기
→ 필요 시 기존 토큰 무효화
→ 사용자 재인증
→ 보안사고 조사
→ 감사 증적 보관

Private Key 유출은 단순 비밀번호 변경 수준의 문제가 아닙니다.

공격자가 유효한 토큰을 직접 만들 수 있기 때문입니다.

## **제40장 요약**

학습 목표 | 40장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Private Key
\= 토큰 발급 서버만 보유

Public Key
\= Gateway와 업무 WAR가 검증에 사용

사용자별 Private Key는 필요하지 않다.

여러 WAR가 같은 발급자를 신뢰하면
같은 Public Key 집합을 사용할 수 있다.

키 교체를 위해 kid와 JWKS를 사용한다.

# **제41장. Gateway 인증과 라우팅**

학습 목표 | 41장. Gateway 인증과 라우팅의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **41.1 Gateway의 역할**

Gateway는 외부 요청이 업무 WAR에 도달하기 전 공통 진입점 역할을 합니다.

사용자
→ Gateway
→ 업무 WAR

주요 책임:

JWT 검증
Route 결정
Header 정규화
외부 접근통제
요청 크기 제한
Rate Limit
Correlation ID
공통 보안로그

Gateway가 업무 규칙까지 처리해서는 안 됩니다.

## **41.2 Gateway 인증 흐름**

요청 수신
→ Authorization Header 확인
→ Bearer Token 추출
→ JWT 형식 확인
→ kid 확인
→ Public Key 선택
→ 서명 검증
→ issuer·audience·exp 검증
→ AuthenticationContext 생성
→ Route 결정
→ 업무 WAR 전달

## **41.3 Authorization Header**

표준 형식:

Authorization: Bearer eyJ...

금지:

URL Query String에 Token 전달

/token=eyJ...

URL은 Proxy, Browser History, Access Log 등에 남을 수 있습니다.

## **41.4 Gateway Route**

예:

| **요청 경로** | **대상** |
| --- | --- |
| /sv/\*\* | sv-service |
| /ic/\*\* | ic-service |
| /cm/\*\* | cm-service |
| /om/\*\* | tcf-om |

표준 온라인 요청:

POST /sv/online

요청 Header:

businessCode=SV
serviceId=SV.Customer.selectSummary

Gateway는 URL과 업무코드의 정합성을 확인할 수 있습니다.

## **41.5 인증 Context 전달**

Gateway가 검증한 사용자정보를 업무 WAR에 전달하는 방법은 여러 가지가 있습니다.

### **원본 JWT 전달**

Gateway 검증
→ 원본 Authorization Header 전달
→ 업무 WAR가 다시 검증

가장 명확한 2차 방어 방식입니다.

### **내부 서명 Header**

Gateway가 내부 Header 생성
→ Gateway 전용 서명 추가
→ 업무 WAR가 서명 검증

구현 복잡도가 증가합니다.

### **네트워크 신뢰만 사용**

Gateway가 넣은 userId Header를
업무 WAR가 그대로 신뢰

단순하지만 업무 WAR 직접 접근이 가능하면 위험합니다.

권장 기본은 원본 JWT 전달과 업무 WAR의 경량 2차 검증입니다.

## **41.6 Header 위조 방지**

사용자가 다음 Header를 직접 보낼 수 있습니다.

X-User-Id: admin
X-Branch-Id: 999999

Gateway는 외부에서 들어온 내부 인증 Header를 제거한 후 검증된 값으로 다시 작성해야 합니다.

외부 X-User-Id 제거
→ JWT 검증
→ 검증된 sub로 내부 Context 생성

## **41.7 Gateway에서 검증할 항목**

| **항목** | **검증** |
| --- | --- |
| Authorization Header | 존재·형식 |
| Algorithm | RS256 허용 |
| Signature | Public Key 검증 |
| Issuer | 허용 발급자 |
| Audience | Gateway·업무용 |
| Expiration | 만료 여부 |
| Not Before | 사용 가능시각 |
| Token Type | Access Token |
| User Status | 필요 시 추가 확인 |
| Route | 업무코드 정합성 |

## **41.8 Gateway가 처리하지 않을 것**

고객이 존재하는지 확인

캠페인 승인 가능한 상태인지 확인

다른 지점 고객 조회 가능 여부

업무 데이터 조회

DB 트랜잭션 처리

Gateway는 공통 경계이고 업무 Rule은 업무 WAR가 담당합니다.

## **41.9 Gateway 장애**

Gateway가 단일 장애점이 되지 않도록 해야 합니다.

검토사항:

-   다중 인스턴스
-   L4 Load Balancing
-   Health Check
-   설정 배포
-   Route Cache
-   JWKS Cache
-   장애격리
-   처리량과 Thread
-   Access Log 용량

## **41.10 Rate Limit**

비정상적으로 많은 요청을 제한할 수 있습니다.

기준 예:

사용자별
IP별
ServiceId별
채널별

단순히 모든 사용자에게 같은 제한을 적용하면 정상 대량업무를 방해할 수 있습니다.

## **41.11 Gateway 인증 오류 응답**

{
"result": {
"resultStatus": "FAIL",
"resultCode": "E-COM-AUTH-0001",
"message": "인증정보가 유효하지 않습니다."
},
"error": {
"errorType": "AUTHENTICATION"
}
}

토큰이 왜 실패했는지 너무 자세히 외부에 알려주지 않을 수 있습니다.

운영로그에는 다음을 구분합니다.

TOKEN\_MISSING
TOKEN\_EXPIRED
INVALID\_SIGNATURE
INVALID\_ISSUER
INVALID\_AUDIENCE
UNKNOWN\_KID

## **제41장 요약**

학습 목표 | 41장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Gateway는 외부 요청의 공통 보안 경계다.

JWT를 검증하고 Route를 결정하지만
업무 규칙은 처리하지 않는다.

외부에서 들어온 인증 Header를 제거하고
검증된 Context만 전달해야 한다.

# **제42장. Gateway가 없는 경우와 업무 WAR 2차 검증**

학습 목표 | 42장. Gateway가 없는 경우와 업무 WAR 2차 검증의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **42.1 Gateway가 없으면 어떻게 하나요?**

Gateway가 없더라도 인증을 생략할 수는 없습니다.

요청 흐름:

사용자
→ L4·Apache
→ 업무 WAR
→ TCF

각 업무 WAR가 직접 다음 책임을 수행해야 합니다.

Authorization Header 확인
JWT 서명 검증
issuer·audience·만료 확인
AuthenticationContext 생성
STF에 Context 전달
권한 확인

## **42.2 검증 위치**

권장 구조:

업무 WAR
├─ JWT Authentication Filter
├─ AuthenticationContext
├─ OnlineTransactionController
└─ STF 권한 검증

흐름:

HTTP 요청
→ JwtAuthenticationFilter
→ JWT 검증
→ AuthenticationContext 생성
→ Controller
→ TCF
→ STF

인증이 실패하면 Controller와 Handler에 도달하지 않습니다.

## **42.3 업무 WAR별 Filter 중복**

여러 WAR에서 같은 Filter 코드를 복사하면 유지보수가 어렵습니다.

금지:

sv-service에 JWT 검증 코드 복사
ic-service에 JWT 검증 코드 복사
cm-service에 JWT 검증 코드 복사

권장:

tcf-jwt 또는 tcf-web 공통 모듈
→ JwtAuthenticationFilter 제공
→ 각 업무 WAR가 공통 설정으로 적용

## **42.4 Public Key 관리**

Gateway가 없으면 각 업무 WAR가 Public Key를 사용합니다.

tcf-jwt
→ JWKS 또는 Public Key 배포

sv-service
→ Public Key Cache

ic-service
→ Public Key Cache

cm-service
→ Public Key Cache

같은 발급자를 신뢰하면 동일한 Public Key 집합을 사용할 수 있습니다.

## **42.5 직접 접근 방어**

Gateway가 있는 환경에서도 사용자가 업무 WAR의 내부 포트에 직접 접근할 가능성을 검토해야 합니다.

방어계층:

Network ACL
→ 내부 포트 외부 차단

Apache·L4 Route
→ 허용 경로 통제

업무 WAR Filter
→ JWT 재검증

STF
→ Header·권한 정합성 확인

Gateway만 믿고 업무 WAR의 인증을 완전히 제거하면 네트워크 설정 오류 시 우회 가능성이 생깁니다.

## **42.6 2차 검증의 의미**

업무 WAR가 Gateway와 동일한 모든 작업을 다시 할 필요는 없습니다.

최소 검증:

서명
issuer
audience
expiration
사용자 식별자
Token Type

또한 Gateway가 만든 내부 Header와 JWT Claim이 일치하는지 확인할 수 있습니다.

Gateway Header userId
\= JWT sub

## **42.7 Audience 설계**

업무 WAR가 공통 Audience를 사용할 수 있습니다.

aud = nsight-services

업무별로 분리할 수도 있습니다.

aud = nsight-sv
aud = nsight-cm

업무별 Audience는 보안 경계를 강화하지만 발급·운영 복잡도가 증가합니다.

초기에는 공통 업무 Audience와 업무코드 정합성 검증을 조합할 수 있습니다.

## **42.8 STF의 2차 검증**

Filter가 인증을 완료한 뒤 STF는 거래 Header와 인증 Context를 비교합니다.

JWT sub
\= AuthenticationContext userId

Header userId
\= AuthenticationContext userId

Header businessCode
\= URL 업무코드
\= ServiceId Prefix

불일치하면 거래를 차단합니다.

## **42.9 Gateway와 업무 WAR 책임 비교**

| **항목** | **Gateway** | **업무 WAR** |
| --- | --- | --- |
| 외부 JWT 검증 | 주책임 | 2차 검증 |
| Route | 주책임 | 대상 업무 확인 |
| Rate Limit | 주책임 | 필요 시 보조 |
| 기능권한 | 1차 가능 | STF 최종 |
| 데이터권한 | 처리하지 않음 | Service·Rule |
| 업무코드 정합성 | 1차 | 최종 |
| 거래통제 | 선택 | STF |
| 업무 트랜잭션 | 처리하지 않음 | Facade |

Gateway가 없으면 업무 WAR가 Gateway의 인증 책임까지 수행합니다.

## **42.10 금지 예시**

String userId =
request.getHeader("X-User-Id");

AuthenticationContext.setUserId(userId);

서명 검증 없이 Header만으로 인증 Context를 만들면 안 됩니다.

## **제42장 요약**

학습 목표 | 42장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Gateway가 없더라도
각 업무 WAR가 JWT를 직접 검증해야 한다.

Gateway가 있는 경우에도
업무 WAR는 최소 2차 검증과
Header 정합성 확인을 수행한다.

공통 Filter를 모듈화하여
WAR마다 인증코드를 복사하지 않는다.

# **제43장. 세션 제거와 하이브리드 인증 전략**

학습 목표 | 43장. 세션 제거와 하이브리드 인증 전략의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **43.1 세션 제거를 검토하는 이유**

여러 Tomcat과 WAR가 운영되면 서버 세션 공유가 복잡해질 수 있습니다.

Tomcat A에서 로그인
→ 다음 요청이 Tomcat B로 전달
→ Session 없음

해결방식:

-   Sticky Session
-   DeltaManager
-   JDBCStore
-   Spring Session JDBC
-   Redis
-   JWT Stateless

NSIGHT에서 Redis를 사용하지 않는다면 JDBC 기반 Session이나 JWT를 검토할 수 있습니다.

## **43.2 완전한 세션 제거**

구조:

로그인
→ Access Token 발급
→ 이후 모든 요청에 JWT
→ 서버는 HttpSession을 사용하지 않음

장점:

-   WAS 간 Session 복제 불필요
-   수평 확장 단순화
-   업무 WAR 독립성 향상
-   Gateway 인증과 잘 맞음

단점:

-   즉시 로그아웃 어려움
-   권한변경 반영 지연
-   Refresh Token 관리 필요
-   클라이언트 저장 보안
-   토큰 탈취 대응 필요

## **43.3 OM 화면의 세션**

OM은 운영자용 관리화면이며 다음 특성이 있습니다.

관리화면 상태
다단계 메뉴
운영자 작업
통제 변경
승인
감사

따라서 OM 화면은 HttpSession을 유지하고 업무 API 호출에는 JWT를 사용하는 하이브리드 구조를 적용할 수 있습니다.

OM Session
\= 관리화면 로그인 상태

JWT
\= Gateway·업무 API 인증

## **43.4 세션과 JWT 사용자 정합성**

OM Session 사용자와 JWT 사용자가 달라서는 안 됩니다.

Session userId=user01
JWT sub=admin01
→ 차단

필요 검증:

Session Principal
\= JWT Subject

Session 상태
\= ACTIVE

JWT 만료
\= 유효

## **43.5 로그아웃**

하이브리드 로그아웃 흐름:

사용자 로그아웃
→ OM Session 무효화
→ Refresh Token 폐기
→ 필요 시 Access Token Denylist
→ Cookie 삭제
→ 보안로그 기록

Access Token 수명이 짧다면 모든 Access Token을 DB에서 관리하지 않고 만료를 기다리는 전략도 가능합니다.

중요 관리자 토큰은 별도 즉시 폐기 정책을 적용할 수 있습니다.

## **43.6 권한 변경 반영**

사용자의 권한이 변경되었습니다.

기존 JWT
roles=\[SV\_VIEWER\]

운영자가 권한 제거

기존 JWT가 만료될 때까지 권한이 남을 수 있습니다.

대안:

-   Access Token 수명 단축
-   Token Version 검증
-   중요 기능은 서버 권한조회
-   권한변경 시 Refresh Token 폐기
-   중요 관리자 Token Denylist

## **43.7 Token Version**

사용자 테이블:

USER\_ID=user01
TOKEN\_VERSION=4

JWT:

{
"sub": "user01",
"tokenVersion": 4
}

권한 강제 변경 시 사용자 TOKEN\_VERSION을 5로 증가시킵니다.

검증 서버가 현재 Version과 비교하면 이전 토큰을 거부할 수 있습니다.

하지만 매 요청마다 DB를 조회하면 성능과 가용성 부담이 생기므로 Cache를 사용할 수 있습니다.

## **43.8 Session Timeout과 Token Timeout**

예:

OM Session Timeout
60분

Access Token
15분

Refresh Token
8시간 또는 업무정책값

Session이 살아 있어도 Access Token은 갱신이 필요할 수 있습니다.

Access Token이 유효해도 OM Session이 종료되면 관리화면 접근을 차단할 수 있습니다.

## **43.9 다중 Tomcat Session 관리**

OM Session을 여러 Tomcat에서 공유해야 한다면 다음 방식을 검토합니다.

| **방식** | **특징** |
| --- | --- |
| Sticky Session | 간단하지만 장애 시 Session 유실 가능 |
| DeltaManager | Tomcat 간 메모리 복제 |
| JDBCStore | DB 기반 Session 저장 |
| Spring Session JDBC | 애플리케이션 계층 Session 공유 |
| JWT 전환 | 서버 Session 의존 축소 |

Session에 대용량 객체를 저장하면 복제와 DB 부하가 증가합니다.

## **43.10 권장 방향**

일반적인 권장 방향:

업무 API
→ JWT 중심

OM 관리 UI
→ 필요 시 Session + JWT

업무 WAR
→ Session 비의존

사용자·지점·권한
→ AuthenticationContext 사용

업무 코드가 직접 HttpSession을 조회하지 않도록 해야 합니다.

금지:

HttpSession session =
request.getSession();

String userId =
(String) session.getAttribute("USER\_ID");

권장:

String userId =
transactionContext.getUserId();

## **제43장 요약**

학습 목표 | 43장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

업무 API는 JWT 중심으로
서버 Session 의존을 줄일 수 있다.

OM 관리화면은 필요에 따라
Session과 JWT를 함께 사용할 수 있다.

업무 Service는 HttpSession이 아니라
검증된 TransactionContext를 사용한다.

# **제44장. 권한과 AuthenticationContext**

학습 목표 | 44장. 권한과 AuthenticationContext의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **44.1 AuthenticationContext란 무엇인가요?**

AuthenticationContext는 JWT와 Session 검증 결과로 만들어진 신뢰 가능한 사용자 문맥입니다.

예:

public record AuthenticationContext(
String userId,
String branchId,
String organizationCode,
Set<String> roles,
Set<String> authorities,
String authenticationType,
String tokenId
) {
}

## **44.2 TransactionContext와의 관계**

인증 Context는 TCF 거래 Context에 반영됩니다.

AuthenticationContext
├─ userId
├─ branchId
├─ roles
└─ authorities

↓

TransactionContext
├─ GUID
├─ TraceId
├─ ServiceId
├─ transactionCode
├─ userId
├─ branchId
├─ channelId
└─ authorities

업무 계층은 TransactionContext를 통해 사용자정보를 사용합니다.

## **44.3 기능권한**

ServiceId 실행 여부를 판단합니다.

예:

ServiceId
CM.Campaign.approve

필요 권한
CM\_CAMPAIGN\_APPROVE

STF:

authorizationService.check(
context.getAuthorities(),
"CM\_CAMPAIGN\_APPROVE"
);

권한이 없으면 Handler를 실행하지 않습니다.

## **44.4 데이터권한**

기능권한이 있어도 모든 데이터를 볼 수 있는 것은 아닙니다.

예:

SV\_CUSTOMER\_VIEW 권한 보유

하지만
자기 지점 고객만 조회 가능

Service·Rule·SQL에서 데이터 범위를 적용합니다.

WHERE CUSTOMER\_NO = #{customerNo}
AND BRANCH\_ID = #{branchId}

branchId는 인증 Context에서 가져옵니다.

## **44.5 권한을 화면에서만 숨기면 안 된다**

화면에서 승인 버튼을 숨겼다고 서버 권한검사가 필요 없는 것이 아닙니다.

공격자는 직접 API를 호출할 수 있습니다.

화면 버튼 숨김
\= 사용자 편의

서버 권한검사
\= 실제 보안

## **44.6 권한코드 관리**

| **권한코드** | **설명** |
| --- | --- |
| SV\_CUSTOMER\_VIEW | 고객조회 |
| CM\_CAMPAIGN\_CREATE | 캠페인 등록 |
| CM\_CAMPAIGN\_UPDATE | 캠페인 변경 |
| CM\_CAMPAIGN\_APPROVE | 캠페인 승인 |
| OM\_SERVICE\_CONTROL | 거래통제 변경 |

권한코드도 OM 또는 공통 권한 시스템에서 기준정보로 관리합니다.

## **44.7 역할과 권한**

역할
\= 권한 묶음

권한
\= 실제 기능 실행 단위

예:

역할: CM\_MANAGER

포함 권한
CM\_CAMPAIGN\_VIEW
CM\_CAMPAIGN\_CREATE
CM\_CAMPAIGN\_UPDATE
CM\_CAMPAIGN\_APPROVE

프로그램이 역할명을 직접 검사하면 역할구성이 변경될 때 코드수정이 필요합니다.

금지:

if ("CM\_MANAGER".equals(role)) {
approve();
}

권장:

if (authorities.contains(
"CM\_CAMPAIGN\_APPROVE")) {
approve();
}

## **44.8 요청 Header 정합성**

표준 요청 Header:

{
"userId": "user01",
"branchId": "001234"
}

AuthenticationContext:

userId=user01
branchId=001234

두 값이 다르면 처리정책을 정해야 합니다.

권장 엄격모드:

불일치
→ 위변조 가능성
→ 거래 차단
→ 보안로그

또는 Header 사용자값을 무시하고 Context 값으로 보정할 수 있습니다.

금융권 중요 거래에서는 불일치를 탐지하고 차단하는 방식을 우선 검토합니다.

## **44.9 대리업무와 위임**

대리 처리 기능이 있다면 단순히 userId를 바꾸면 안 됩니다.

다음 정보를 분리해야 합니다.

실제 로그인 사용자
actorUserId

대리 대상 사용자
onBehalfOfUserId

위임 근거
delegationId

감사로그:

actor=user01
onBehalfOf=manager01
serviceId=...
delegationId=...

## **44.10 관리자 권한**

관리자라고 모든 업무 데이터를 무제한 조회하도록 만들면 안 됩니다.

관리자 권한도 목적별로 나눕니다.

사용자관리 관리자
거래통제 관리자
보안감사 관리자
업무 데이터 관리자

최소권한 원칙을 적용합니다.

## **44.11 인증·권한 오류 로그**

인증 실패 로그:

event=AUTHENTICATION\_FAIL
reason=TOKEN\_EXPIRED
clientIp=...
channelId=...

권한 실패 로그:

event=AUTHORIZATION\_FAIL
userId=user01
serviceId=CM.Campaign.approve
requiredAuthority=CM\_CAMPAIGN\_APPROVE
traceId=T...

JWT 원문은 로그에 남기지 않습니다.

## **제44장 요약**

학습 목표 | 44장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

AuthenticationContext는
서버가 검증한 사용자정보다.

기능권한은 STF에서,
데이터권한은 Service·Rule·SQL에서 확인한다.

화면이 보낸 userId보다
인증 Context를 신뢰해야 한다.

# **3\. 목표 아키텍처**

## **3.1 Gateway가 있는 구조**

\[사용자\]
│
│ SSO 로그인
▼
\[SSO / IdP\]
│
│ 인증결과
▼
\[tcf-om\]
├─ 내부 사용자 확인
├─ 필요 시 HttpSession
└─ JWT 발급 요청
│
▼
\[tcf-jwt\]
Private Key 서명
│
▼
Access Token
│
▼
\[사용자 화면\]
│
│ Authorization: Bearer JWT
▼
\[Gateway\]
├─ Public Key 검증
├─ issuer·audience·exp
├─ 내부 Header 정규화
└─ Route
│
▼
\[업무 WAR\]
├─ JWT 2차 검증
├─ AuthenticationContext
├─ OnlineTransactionController
└─ TCF / STF
├─ Header 정합성
├─ 기능권한
├─ 거래통제
└─ TransactionContext
│
▼
Handler → Facade → Service
│
▼
데이터권한

## **3.2 Gateway가 없는 구조**

\[사용자 화면\]
│
│ JWT
▼
\[L4 / Apache\]
│
▼
\[업무 WAR\]
├─ JwtAuthenticationFilter
├─ Public Key Cache
├─ AuthenticationContext
├─ Controller
└─ TCF / STF

# **4\. 표준 형식**

## **4.1 JWT Claim 예시**

{
"iss": "nsight-auth",
"sub": "user01",
"aud": \[
"nsight-services"
\],
"iat": 1784239200,
"exp": 1784240100,
"jti": "c8bf85e3-4bb3-4f2d",
"branchId": "001234",
"organizationCode": "ORG001",
"roles": \[
"SV\_VIEWER"
\],
"authType": "SSO",
"tokenVersion": 4
}

## **4.2 인증 오류 응답**

{
"result": {
"resultStatus": "FAIL",
"resultCode": "E-COM-AUTH-0001",
"message": "인증정보가 유효하지 않습니다."
},
"body": null,
"error": {
"errorType": "AUTHENTICATION",
"retryable": false
}
}

## **4.3 권한 오류 응답**

{
"result": {
"resultStatus": "FAIL",
"resultCode": "E-COM-AUZ-0001",
"message": "해당 기능을 사용할 권한이 없습니다."
},
"body": null,
"error": {
"errorType": "AUTHORIZATION",
"retryable": false
}
}

# **5\. 구성요소 및 속성**

| **구성요소** | **핵심 속성** |
| --- | --- |
| SSO·IdP | 인증방식, 발급자, 서명 |
| OM\_USER | 사용자 상태, 지점, 조직 |
| tcf-jwt | Issuer, Private Key, 만료시간 |
| JWKS | kid, Public Key |
| Gateway | 검증, Route, Header 정규화 |
| 업무 WAR Filter | JWT 2차 검증 |
| AuthenticationContext | 사용자·역할·권한 |
| STF | 기능권한·Header 정합성 |
| Service·Rule | 데이터권한 |
| Session Store | Session ID, 만료 |
| Refresh Store | Token 상태, 재사용 탐지 |
| Security Log | 인증·권한 실패 |

# **6\. 책임 경계와 RACI**

| **활동** | **SEC** | **AA** | **FW** | **DEV** | **OM** | **OPS** | **QA** | **SSO 담당** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 인증정책 | A | C | C | I | C | C | C | R |
| JWT Claim | A | C | R | C | C | I | C | C |
| Private Key 관리 | A | I | C | I | I | R/C | I | C |
| JWKS 운영 | A | C | R | I | I | R | C | I |
| Gateway 검증 | C | A | R | C | I | C | C | I |
| WAR 2차 검증 | C | A | R | C | I | C | C | I |
| 기능권한 | A/C | C | C | R | R/C | I | C | I |
| 데이터권한 | C | A | C | R | C | I | C | I |
| OM Session | C | C | R | C | A | R/C | C | I |
| 토큰 만료정책 | A | C | R | I | C | C | C | C |
| 보안 테스트 | A/C | C | C | C | I | C | R | C |
| 사고 대응 | A | C | C | I | C | R | I | C |

R = 수행
A = 최종 책임
C = 협의
I = 공유

# **7\. 정상 처리 흐름**

1\. 사용자가 SSO 로그인

2\. SSO가 사용자를 인증

3\. tcf-om이 SSO 결과 검증

4\. 내부 사용자와 계정상태 확인

5\. 필요 시 OM HttpSession 생성

6\. tcf-jwt가 Access·Refresh Token 발급

7\. 화면이 Access Token으로 업무 요청

8\. Gateway가 JWT 검증

9\. Gateway가 업무 WAR로 Route

10\. 업무 WAR가 JWT 2차 검증

11\. AuthenticationContext 생성

12\. STF가 Header와 사용자 정합성 확인

13\. STF가 ServiceId 기능권한 확인

14\. Handler 이하 업무 실행

15\. Service·Rule·SQL이 데이터권한 확인

16\. ETF가 표준 응답과 감사로그 처리

# **8\. 오류·Timeout·장애 흐름**

## **8.1 JWT 없음**

Authorization Header 없음
→ 인증 Filter 차단
→ Controller 미실행
→ 인증 오류

## **8.2 JWT 만료**

exp 초과
→ Access Token 거부
→ Refresh 가능 여부 판단
→ 필요 시 재로그인

## **8.3 서명 위조**

Signature 검증 실패
→ 즉시 차단
→ 보안로그
→ 반복 시 경보

## **8.4 잘못된 Audience**

다른 시스템용 토큰
→ NSIGHT 업무 호출
→ 거부

## **8.5 권한 없음**

인증 성공
→ ServiceId 권한 없음
→ Handler 미실행
→ 권한 오류·감사로그

## **8.6 Header 위변조**

JWT sub=user01
Header userId=admin
→ 불일치
→ 차단·보안로그

## **8.7 JWKS 장애**

Cache Key 존재
→ 검증 계속

새 kid + JWKS 장애
→ 토큰 거부
→ 운영 경보

## **8.8 Private Key 유출**

키 폐기
→ 신규 Key 발급
→ 토큰 발급 전환
→ 기존 토큰 무효화 검토
→ 사용자 재인증

# **9\. 정상 예시**

SSO 사용자
user01

OM 사용자
ACTIVE

JWT
sub=user01
branchId=001234
aud=nsight-services
exp=유효

Gateway
서명 검증 성공

업무 WAR
2차 검증 성공

Header
userId=user01
branchId=001234

ServiceId 권한
SV\_CUSTOMER\_VIEW 보유

결과
업무 거래 실행

# **10\. 금지 예시**

## **10.1 요청 userId 신뢰**

String userId =
request.getHeader().getUserId();

## **10.2 JWT Payload만 읽고 서명 미검증**

String userId =
decodePayload(token).getSubject();

## **10.3 Private Key를 모든 WAR에 배포**

sv-service Private Key 보유
ic-service Private Key 보유
cm-service Private Key 보유

## **10.4 JWT를 URL로 전달**

/sv/online?token=eyJ...

## **10.5 Local Storage에 장기 Refresh Token 저장**

localStorage.setItem(
"refreshToken",
refreshToken
);

## **10.6 화면 버튼 숨김만으로 권한 통제**

승인 버튼 숨김
→ 서버 권한검사 없음

## **10.7 Gateway만 믿고 내부 포트 공개**

업무 WAR 직접 접근 가능
JWT Filter 없음

# **11\. 연계 규칙**

다른 업무 WAR를 호출할 때도 인증 문맥을 유지해야 합니다.

원본 사용자
user01

호출 업무
SV → IC

전달 정보
GUID
TraceId
사용자 식별자
Service Token 또는 사용자 Token
호출 ServiceId

내부 서비스 호출 방식은 두 가지를 검토할 수 있습니다.

### **사용자 Token 전달**

최종 사용자 권한을 그대로 검증

### **내부 Service Token**

호출 서비스의 신원 검증
\+ 원 사용자정보 별도 전달

중요한 것은 실제 사용자와 호출 시스템을 구분해 감사할 수 있어야 한다는 점입니다.

# **12\. 데이터 및 상태관리**

## **12.1 Refresh Token 상태**

ACTIVE
→ ROTATED
→ REVOKED
→ EXPIRED

## **12.2 사용자 상태**

ACTIVE
LOCKED
SUSPENDED
EXPIRED
DELETED

## **12.3 Key 상태**

GENERATED
ACTIVE
RETIRING
RETIRED
COMPROMISED

상태 변경은 작업자와 승인자를 기록해야 합니다.

# **13\. 성능·용량·확장성**

| **영역** | **고려사항** |
| --- | --- |
| JWT 검증 | Public Key Cache |
| JWKS | 매 요청 호출 금지 |
| 권한정보 | Token 크기 제한 |
| Session | 대용량 객체 저장 금지 |
| 다중 WAR | 공통 Filter 모듈 |
| 권한 조회 | Cache와 변경 반영 |
| Denylist | 모든 요청 DB 조회 방지 |
| Refresh Store | 인덱스·보관기간 |
| 보안로그 | 반복 공격 로그 폭증 방지 |
| Gateway | 인증 검증 처리량 확보 |

# **14\. 보안·개인정보·감사**

JWT 원문을 일반 로그에 기록하지 않는다.

Private Key 접근권한을 최소화한다.

Refresh Token은 Access Token보다 강하게 보호한다.

사용자 ID와 지점 ID의 위변조를 검사한다.

관리자 권한변경과 거래통제는 감사로그를 남긴다.

토큰 실패가 반복되면 공격 가능성을 경보한다.

사용자 권한은 최소권한 원칙으로 부여한다.

# **15\. 운영·모니터링·장애 대응**

운영 지표:

| **지표** | **의미** |
| --- | --- |
| 로그인 성공률 | SSO·OM 정상 여부 |
| JWT 발급 실패 | tcf-jwt 상태 |
| TOKEN\_EXPIRED | 만료 정책·사용행태 |
| INVALID\_SIGNATURE | 위조·키 불일치 |
| UNKNOWN\_KID | 키 배포·Rotation 문제 |
| 권한 실패율 | 권한설정·비정상 접근 |
| Refresh 실패 | Refresh Store 문제 |
| Session 수 | OM Session 용량 |
| JWKS 갱신 실패 | Key 서비스 장애 |
| Gateway 인증시간 | 검증 부하 |

# **16\. 자동검증 및 품질 Gate**

| **Gate** | **검증** |
| --- | --- |
| JWT | 허용 Algorithm 고정 |
| Claim | issuer·audience·exp 검증 |
| Key | Private Key 소스 저장 금지 |
| JWKS | kid 존재 |
| Header | 외부 내부인증 Header 제거 |
| Context | 요청 userId 직접 사용 금지 |
| 권한 | ServiceId별 권한 등록 |
| WAR | 공통 JWT Filter 적용 |
| Session | 민감정보 저장 금지 |
| 로그 | JWT·Refresh Token 미노출 |
| 테스트 | 위조·만료·우회 테스트 |
| Rotation | 구·신 Key 동시검증 테스트 |

# **17\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| AUTH-001 | 정상 SSO 로그인 | JWT 발급 |
| AUTH-002 | 위조 SSO 결과 | 로그인 차단 |
| AUTH-003 | 내부 사용자 없음 | 접근 차단 |
| AUTH-004 | 잠금 사용자 | JWT 미발급 |
| AUTH-005 | 정상 Access Token | 거래 성공 |
| AUTH-006 | Token 없음 | 인증 오류 |
| AUTH-007 | 서명 위조 | 차단·보안로그 |
| AUTH-008 | Token 만료 | 갱신 안내 |
| AUTH-009 | 잘못된 issuer | 차단 |
| AUTH-010 | 잘못된 audience | 차단 |
| AUTH-011 | unknown kid | JWKS 갱신 후 차단·검증 |
| AUTH-012 | Gateway 우회 접근 | WAR Filter 차단 |
| AUTH-013 | Header userId 위조 | 정합성 오류 |
| AUTH-014 | 기능권한 없음 | Handler 미실행 |
| AUTH-015 | 데이터권한 없음 | 업무 권한 오류 |
| AUTH-016 | Refresh 정상 | 새 Token 발급 |
| AUTH-017 | 폐기 Refresh 재사용 | 차단·경보 |
| AUTH-018 | Session 사용자와 JWT 불일치 | 차단 |
| AUTH-019 | 기존·신규 Key 공존 | 모두 검증 |
| AUTH-020 | 기존 Key 제거 후 만료 Token | 정책대로 거부 |
| AUTH-021 | JWKS 장애·Cache Key 존재 | 검증 계속 |
| AUTH-022 | JWKS 장애·새 kid | 거부 |
| AUTH-023 | JWT 원문 로그 검사 | 미노출 |
| AUTH-024 | 관리권한 변경 | 감사로그 기록 |
| AUTH-025 | 다른 WAR 동일 Public Key | 정상 검증 |

# **18\. 제6부 체크리스트**

## **18.1 로그인**

| **점검 항목** | **확인** |
| --- | --- |
| SSO 결과의 서명을 검증하는가? | □ |
| 내부 사용자 상태를 확인하는가? | □ |
| 잠금·중지 사용자의 JWT 발급을 차단하는가? | □ |
| 로그인 실패를 보안로그에 남기는가? | □ |
| SSO 원본 Token을 Session에 저장하지 않는가? | □ |

## **18.2 JWT**

| **점검 항목** | **확인** |
| --- | --- |
| RS256 등 허용 Algorithm을 고정했는가? | □ |
| issuer를 검증하는가? | □ |
| audience를 검증하는가? | □ |
| exp와 nbf를 검증하는가? | □ |
| Access·Refresh Token을 구분하는가? | □ |
| JWT에 민감정보가 없는가? | □ |
| Token 수명이 정책과 일치하는가? | □ |

## **18.3 키 관리**

| **점검 항목** | **확인** |
| --- | --- |
| Private Key가 발급 서버에만 존재하는가? | □ |
| 소스·YAML에 Private Key가 없는가? | □ |
| Public Key Cache가 있는가? | □ |
| kid와 JWKS를 사용하는가? | □ |
| 키 교체 절차가 있는가? | □ |
| 유출 시 폐기 절차가 있는가? | □ |

## **18.4 Gateway와 업무 WAR**

| **점검 항목** | **확인** |
| --- | --- |
| Gateway가 외부 인증 Header를 제거하는가? | □ |
| Gateway가 JWT를 검증하는가? | □ |
| 업무 WAR가 2차 검증하는가? | □ |
| 직접 접근 포트가 보호되는가? | □ |
| Gateway가 없을 때 WAR Filter가 동작하는가? | □ |
| 공통 Filter가 모듈화되어 있는가? | □ |

## **18.5 권한**

| **점검 항목** | **확인** |
| --- | --- |
| 인증과 인가를 구분하는가? | □ |
| ServiceId별 기능권한이 있는가? | □ |
| 데이터권한을 별도로 확인하는가? | □ |
| 화면 숨김만으로 권한을 처리하지 않는가? | □ |
| 역할이 아닌 권한코드 중심으로 검사하는가? | □ |
| 관리자 권한도 분리되어 있는가? | □ |

## **18.6 세션**

| **점검 항목** | **확인** |
| --- | --- |
| 업무 Service가 HttpSession에 의존하지 않는가? | □ |
| Session에 대용량 객체가 없는가? | □ |
| Session과 JWT 사용자를 비교하는가? | □ |
| 로그아웃 시 Session과 Refresh Token을 정리하는가? | □ |
| 다중 Tomcat Session 정책이 정의되어 있는가? | □ |

## **18.7 로그와 감사**

| **점검 항목** | **확인** |
| --- | --- |
| JWT 원문이 로그에 없는가? | □ |
| 인증 실패 사유를 내부적으로 구분하는가? | □ |
| 권한 실패에 TraceId가 있는가? | □ |
| 관리자 권한변경을 감사하는가? | □ |
| Private Key 접근이 감사되는가? | □ |
| 반복 위조 시도를 경보하는가? | □ |

# **19\. 변경·호환성·폐기 관리**

## **19.1 Claim 변경**

기존 Claim을 제거하거나 의미를 변경하면 Gateway와 모든 업무 WAR에 영향을 줍니다.

신규 Claim 추가
→ 하위 호환성 검토

Claim 삭제
→ 검증 서버 영향 확인

Claim 의미 변경
→ Token Version 또는 신규 Claim

## **19.2 발급자 변경**

iss가 변경되면 모든 검증 서버 설정을 변경해야 합니다.

전환기간에는 구·신 발급자 지원 여부를 명확히 결정해야 합니다.

## **19.3 Audience 변경**

Audience 변경 대상:

Gateway
업무 WAR
외부 연계
테스트
모니터링

새 Audience를 먼저 검증 서버에 배포한 뒤 발급을 전환하는 순서를 사용합니다.

## **19.4 키 폐기**

ACTIVE
→ RETIRING
→ RETIRED

유출된 Key:

ACTIVE
→ COMPROMISED
→ 즉시 폐기

폐기 전 기존 Token의 최대 만료시간을 확인합니다.

## **19.5 Session 방식 폐기**

기존 업무 코드가 HttpSession을 직접 참조하고 있다면 다음 순서로 전환합니다.

Session 사용자 조회 위치 파악
→ AuthenticationContext로 대체
→ 병행 운영
→ 회귀 테스트
→ Session 의존 제거
→ 설정 폐기

# **20\. 시사점**

## **20.1 핵심 아키텍처 판단**

인증구조의 핵심은 Token을 발급하는 것 자체가 아닙니다.

신뢰할 수 있는 사용자정보를 만들고

그 정보가 업무 WAR까지
변조되지 않은 상태로 전달되며

각 계층이 필요한 권한을
다시 확인하는 것

이 핵심입니다.

## **20.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| 요청 userId 신뢰 | 사용자 위변조 |
| JWT 서명 미검증 | 임의 사용자 사칭 |
| issuer·audience 미검증 | 다른 시스템 Token 오용 |
| 긴 Access Token 수명 | 탈취 피해 확대 |
| Refresh Token 노출 | 장기간 계정 탈취 |
| Private Key 다중 배포 | 위조 Token 발급 위험 |
| Gateway 단일 검증 | 직접 접근 우회 |
| 권한을 화면에서만 처리 | API 직접 호출 |
| JWT 권한정보 장기 유지 | 권한회수 지연 |
| Session 대용량 저장 | 복제·DB 부하 |
| Key Rotation 미구현 | 키 교체 장애 |
| JWT 원문 로그 | Token 유출 |

## **20.3 우선 보완 과제**

1\. SSO 검증 기준 확정
2\. 내부 사용자 상태체계 정리
3\. JWT Claim 최소화
4\. Access·Refresh Token 정책 확정
5\. RS256 Private Key 보호
6\. JWKS·kid·Rotation 구현
7\. Gateway JWT 검증
8\. 업무 WAR 공통 2차 Filter 적용
9\. AuthenticationContext 표준화
10\. ServiceId 기능권한 연결
11\. Session 직접 참조 제거
12\. 보안 테스트와 감사로그 강화

## **20.4 중장기 발전 방향**

기본 JWT 검증
→ JWKS 자동 갱신
→ Refresh Rotation
→ Token 재사용 탐지
→ Token Version
→ 위험기반 인증
→ 단계적 추가 인증
→ 서비스 간 인증
→ 키 HSM 관리
→ 통합 보안관제

# **21\. 마무리말**

제6부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

인증
\= 사용자가 누구인지 확인

인가
\= 무엇을 할 수 있는지 확인

세션
\= 서버가 로그인 상태를 기억

JWT
\= 검증된 인증정보를 서명해 전달

Private Key
\= 토큰 발급

Public Key
\= 토큰 검증

NSIGHT의 권장 인증 흐름은 다음과 같습니다.

SSO
→ tcf-om 사용자 확인
→ tcf-jwt 토큰 발급
→ Gateway 검증
→ 업무 WAR 2차 검증
→ AuthenticationContext 생성
→ STF 기능권한
→ Service·Rule 데이터권한

Gateway가 없을 때는 다음과 같이 바뀝니다.

SSO
→ JWT 발급
→ 업무 WAR JWT Filter
→ AuthenticationContext
→ STF
→ 업무 처리

초보 개발자가 인증기능을 구현할 때 마지막으로 확인해야 할 질문은 다음과 같습니다.

이 사용자정보는 누가 검증했는가?

화면이 보낸 userId를 믿고 있지는 않은가?

Token의 서명을 확인했는가?

issuer와 audience를 확인했는가?

Token이 만료되지 않았는가?

Gateway를 우회해도 차단되는가?

업무 WAR가 Public Key로 검증하는가?

ServiceId 권한이 있는가?

데이터 범위 권한도 확인하는가?

Private Key가 안전하게 보호되는가?

로그에 JWT 원문이 남지 않는가?

키 교체 시 무중단 검증이 가능한가?

이 질문에 답할 수 있다면 로그인 기능을 단순히 연결하는 수준을 넘어, 사용자 신원과 업무 권한을 끝까지 안전하게 보호하는 개발자가 될 수 있습니다.
