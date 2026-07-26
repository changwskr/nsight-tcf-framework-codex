# P11 — CI/CD·배포·Rollback 프롬프트

사용: `P0` + `P11` + 모듈·환경·배포도구 입력

```text
[P11 CI/CD·배포 작업]

다음 모듈의 빌드·배포·Rollback 체계를 설계하거나 검토하라.

모듈: {모듈}
Branch 전략: {전략}
빌드도구: {Gradle}
배포형태: {WAR/JAR}
Tomcat 구조: {구조}
환경: {dev/test/stage/prod}
배포도구: {Jenkins/GitLab 등}
운영중단 허용시간: {시간}
Rollback 목표시간: {시간}

Pipeline을 다음 단계로 구성하라.

1. Checkout
2. JDK·Gradle 버전 확인
3. Clean Build
4. 단위테스트
5. 구조·명명·ServiceId 검증
6. Mapper·SQL 검증
7. Secret·Dependency Scan
8. 통합테스트
9. WAR 생성
10. Artifact Version·Checksum 생성
11. 환경설정 검증
12. 배포 전 Backup
13. 배포
14. Health Check
15. Smoke Test
16. 표준 거래 호출
17. OM 등록 정합성 확인
18. 실패 시 자동 또는 수동 Rollback
19. 배포 결과와 로그 보관

필수 검증:

- Commit ID와 Artifact 연결
- WAR명과 Context Path 일치
- Profile 설정 분리
- Secret 미포함
- DB Migration 선후관계
- Gateway Route 반영
- OM Catalog 반영
- Timeout 정책 반영
- 배포 전후 ServiceId 비교
- Rollback 시 DB 호환성
- 다중 WAR 재기동 영향

최종적으로 배포 Runbook과 Go/No-Go 체크리스트를 작성하라.
```
