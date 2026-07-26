# P7 — JWT·SSO·권한·감사 설계 프롬프트

사용: `P0` + `P7` + 채널·IdP·Gateway·권한 모델

```text
[P7 인증·보안 설계 작업]

다음 채널과 시스템의 인증·권한 구조를 설계하라.

채널: {WEBTOPSUITE/React/외부 API}
SSO 또는 IdP: {시스템}
Gateway 존재 여부: {Y/N}
토큰 발급 시스템: {tcf-jwt/별도 인증 WAS}
업무 WAR: {대상}
관리자 시스템 여부: {Y/N}
세션 사용 여부: {사용/제거}
권한 모델: {역할/기능/데이터 권한}
개인정보: {대상}
감사 대상 거래: {대상}

다음 책임을 분리하라.

- SSO: 원천 사용자 인증
- tcf-jwt: Access·Refresh Token 발급·갱신·폐기
- Gateway: 외부 요청 JWT 1차 검증과 라우팅
- 업무 WAR JWT Filter: Gateway가 없거나 우회 가능할 때 검증
- STF: 인증 문맥과 Header 정합성·공통 권한 검증
- 업무 Service·Rule: 상세 업무 권한과 데이터 권한
- tcf-om: 사용자·권한·강제 로그아웃·인증 운영관리

다음 흐름을 작성하라.

1. 로그인·SSO
2. Access Token 발급
3. Refresh Token 발급·저장
4. Gateway 검증
5. 업무 WAR 검증
6. 권한 검증
7. Token 만료
8. Refresh
9. 로그아웃
10. 강제폐기
11. 권한 변경
12. 키 Rotation
13. Gateway 장애·미경유
14. 인증 실패 감사로그

검토 항목:

- iss·aud·exp·nbf·jti
- Private Key 보관
- JWKS와 Public Key Cache
- Refresh Token Hash·Rotation
- DenyList 또는 Token Version
- Header 위조 방지
- sessionStorage·postMessage 사용 시 origin 검증
- URL·로그·localStorage Token 노출 금지
- 개인정보 마스킹
- 관리자 중요행위 감사

정상·공격·만료·위조·재사용·권한변경 테스트를 포함하라.
```
