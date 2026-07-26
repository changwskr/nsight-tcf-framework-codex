# 보안 점검표

- [ ] `JSESSIONID` Cookie에 `HttpOnly`, `Secure`, `SameSite=Lax` 적용
- [ ] Apache ServerTokens Prod / ServerSignature Off
- [ ] TLS 1.0/1.1 비활성화
- [ ] `/server-status`, `/balancer-manager` 관리자망 제한
- [ ] 로그에 주민번호, 계좌번호, 고객 상세정보 원문 저장 금지
- [ ] DB 계정은 업무별 최소권한 적용
- [ ] 외부 연계 Timeout 및 Circuit Breaker 적용
- [ ] 운영 비밀번호는 환경변수 또는 Vault에서 주입
