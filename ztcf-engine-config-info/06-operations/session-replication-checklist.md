# Session Replication 점검표

SoT: [../10-operations-셋팅정보.md](../10-operations-셋팅정보.md)

- [ ] core(+jwt) WAR 전체에 `WEB-INF/web.xml`의 `<distributable/>` 포함
- [ ] 세션 저장 객체가 모두 `Serializable` 구현
- [ ] `jvmRoute`가 노드별로 유일함: `tc01`, `tc02`, `tc03`
- [ ] Apache `BalancerMember route`와 Tomcat `jvmRoute`가 일치
- [ ] 세션에 고객조회 결과, Single View(`/sv`) 결과, 대량 DTO 저장 금지
- [ ] 세션 크기 2KB 이하 권장, 최대 5KB 이하
- [ ] DeltaManager는 센터 내부 Cluster에만 적용
- [ ] 센터 장애 시 재로그인 정책 확인
- [ ] AP 장애 테스트: 로그인 후 노드 중지, 다른 노드에서 세션 유지 확인
