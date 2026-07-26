# NSIGHT Spring JDBC Session 전체 설정 파일

> **참조 구성:** 17 WAR 목표 아키텍처를 설명하는 설정 자료이며, 현재 저장소의 ztomcat 16 WAR 배포 목록과는 별도입니다.

## 변경 기준
기존 Redis Session 기준을 제거하고, Spring Session JDBC 기반으로 세션을 관리합니다.

## 구조
nh.marketing.com
  ↓
Apache
  ↓
Tomcat Cluster
  ↓
17 WAR
  ↓
Spring Boot
  ↓
Spring Session JDBC
  ↓
SESSION DB

## 핵심 원칙
- Redis 미사용
- Spring Session JDBC 사용
- Tomcat DeltaManager 미사용 권장
- 단일 도메인 기준 JSESSIONID 쿠키 사용
- 모든 WAR가 동일 SESSION DB를 바라봄
- 세션 데이터는 최소화
- 대량 조회 결과, 고객목록, 캠페인 대상자 목록은 세션 저장 금지

## 포함 파일
- spring/config/common/application.yml
- spring/config/common/application-session-jdbc.yml
- spring/config/common/application-datasource.yml
- spring/config/common/application-mybatis.yml
- spring/config/common/application-transaction.yml
- spring/config/common/application-security.yml
- spring/config/common/application-management.yml
- spring/config/common/application-cache.yml
- spring/config/dev/application-dev.yml
- spring/config/stg/application-stg.yml
- spring/config/prd/application-prd.yml
- spring/sql/spring-session-schema-oracle.sql
- spring/logback/logback-spring.xml
- spring/templates/web.xml
