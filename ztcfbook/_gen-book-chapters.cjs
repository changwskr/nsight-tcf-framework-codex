#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const root = __dirname;

/** @type {{ dir: string, partTitle: string, chapters: Array<{ no: string|number, title: string, slug: string, sections: Array<{ id: string, title: string, sources: string[] }> }> }} */
const BOOK = [
  {
    dir: '서문',
    partTitle: '서문',
    chapters: [
      {
        no: 0,
        title: '서문',
        slug: '00-서문',
        sections: [
          { id: '0.1', title: '이 책의 범위와 읽는 법', sources: ['znsight-man/01-문서개요.md', 'zman/01-문서개요.md'] },
          { id: '0.2', title: '문서 계층과 역할 (설계서 → 아키텍처 → 가이드 → 매뉴얼)', sources: ['zarchitecture/README.md', 'zman/README.md'] },
          { id: '0.3', title: '설계서 vs 코드베이스 차이 (Handler, OM, EAI)', sources: ['zman/00-설계서-코드베이스-대조표.md'] },
          { id: '0.4', title: '용어·약어 표', sources: ['docs/architecture/53-naming-conventions.md', 'znsight-man/부록A-업무코드-표준표.md', 'znsight-man/부록B-ServiceId-명명규칙.md', 'znsight-man/부록C-거래코드-명명규칙.md'] },
        ],
      },
    ],
  },
  {
    dir: '제01편',
    partTitle: '제1편 · TCF Framework 이해하기',
    chapters: [
      { no: 1, title: 'NSIGHT TCF란 무엇인가', slug: '01-NSIGHT-TCF란-무엇인가', sections: [
        { id: '1.1', title: 'TCF의 목적과 핵심 원칙', sources: ['znsight-man/03-TCF-개발원칙.md', 'zman/05-TCF처리구조.md'] },
        { id: '1.2', title: 'Handler 중심 개발 · 공통 파이프라인 · 업무 WAR', sources: ['docs/architecture/architecture.md'] },
        { id: '1.3', title: 'REST가 아닌 Online Endpoint 방식', sources: ['znsight-man/22-Online-Endpoint-기준.md', 'zman/06-표준전문구조.md'] },
        { id: '1.4', title: 'bootRun vs Tomcat WAR (이중 배포)', sources: ['znsight-man/10-bootRun-Tomcat-WAR-차이.md', 'ztomcat/README.md'] },
        { id: '1.5', title: '개발자 역할과 RACI', sources: ['znsight-man/05-개발자-역할과-책임.md'] },
      ]},
      { no: 2, title: '전체 시스템 구조', slug: '02-전체-시스템-구조', sections: [
        { id: '2.1', title: 'GSLB → Gateway → WAR → TCF 흐름', sources: ['zarchitecture/01-전체-시스템-아키텍처.md'] },
        { id: '2.2', title: '채널·UI·외부 연계', sources: ['zarchitecture/13-UI-채널-아키텍처.md', 'zguide/tcf-ui-개발가이드.md'] },
        { id: '2.3', title: '9개 업무 WAR + 플랫폼 모듈 맵', sources: ['zarchitecture/04-업무-도메인-서비스-아키텍처.md', 'zman/04-모듈구성.md'] },
        { id: '2.4', title: '로컬 포트·Context Path 레퍼런스', sources: ['zarchitecture/16-모듈-포트-의존성-레퍼런스.md', 'zguide/README.md'] },
        { id: '2.5', title: 'Gradle 멀티 모듈 의존 그래프', sources: ['docs/architecture/48-multi-module-dependencies.md', 'znsight-man/08-Gradle-멀티모듈.md'] },
      ]},
      { no: 3, title: 'TCF 처리 엔진', slug: '03-TCF-처리-엔진', sections: [
        { id: '3.1', title: 'STF → Dispatcher → ETF 파이프라인', sources: ['docs/architecture/33-TCF.md', 'docs/architecture/34-STF.md', 'docs/architecture/35-BTF.md', 'docs/architecture/36-ETF.md', 'zarchitecture/02-TCF-프레임워크-아키텍처.md'] },
        { id: '3.2', title: 'ServiceId Dispatcher와 Handler Registry', sources: ['zman/07-ServiceIdDispatcher.md', 'docs/architecture/03-transaction.md'] },
        { id: '3.3', title: 'TransactionContext · MDC · 추적', sources: ['docs/architecture/37-transaction-log.md', 'znsight-man/35-거래로그-감사로그-기준.md'] },
        { id: '3.4', title: '표준 전문(준문) Header / Body / Result', sources: ['docs/architecture/02-junmun.md', 'znsight-man/20-표준-전문-구조.md'] },
        { id: '3.5', title: 'Spring Boot 기동 · AutoConfiguration · AOP', sources: ['docs/architecture/30-springboot.md', 'docs/architecture/31-autoconfiguration.md', 'docs/architecture/32-AOP.md', 'zguide/tcf-core-개발가이드.md'] },
      ]},
      { no: 4, title: '애플리케이션 6계층', slug: '04-애플리케이션-6계층', sections: [
        { id: '4.1', title: 'Handler → Facade → Service → Rule → DAO → Mapper', sources: ['docs/architecture/01-application-layer.md', 'zarchitecture/03-애플리케이션-6계층-아키텍처.md'] },
        { id: '4.2', title: '계층별 책임·금지 사항', sources: ['znsight-man/12-애플리케이션-계층구조.md'] },
        { id: '4.3', title: 'Controller를 만들지 않는 이유', sources: ['zguide/README.md', 'zman/08-업무Handler개발.md'] },
        { id: '4.4', title: 'Facade 계층 설계', sources: ['docs/architecture/29-facade.md', 'znsight-man/24-Facade-개발.md'] },
        { id: '4.5', title: '트랜잭션·예외·로그 계층 기준', sources: ['docs/architecture/03-transaction.md', 'docs/architecture/05-exception.md', 'znsight-man/32-예외처리-기준.md', 'znsight-man/36-트랜잭션-기준.md'] },
      ]},
    ],
  },
  {
    dir: '제02편',
    partTitle: '제2편 · 개발 표준과 명명규칙',
    chapters: [
      { no: 5, title: '개발 표준 총정리', slug: '05-개발-표준-총정리', sections: [
        { id: '5.1', title: '개발 표준 전체 요약', sources: ['znsight-man/04-개발표준-전체요약.md'] },
        { id: '5.2', title: '명명규칙 최상위 10원칙', sources: ['znsight-man/14-명명-규칙.md', 'znsight-man/명명규칙-02-최상위-원칙.md'] },
        { id: '5.3', title: '업무코드 · Context · WAR · Package', sources: ['znsight-man/15-업무코드-Context-WAR.md', 'znsight-man/명명규칙-03-업무코드-Context-WAR-Package.md', 'znsight-man/명명규칙-04-업무코드-표준표.md'] },
        { id: '5.4', title: 'Gradle 모듈·패키지 표준', sources: ['znsight-man/09-업무-WAR-구조.md', 'znsight-man/13-패키지-구조-표준.md', 'znsight-man/명명규칙-05-모듈-설계기준.md', 'znsight-man/명명규칙-06-Package.md'] },
        { id: '5.5', title: 'Git 브랜치·Commit·MR 기준', sources: ['znsight-man/07-Git-브랜치-기준.md'] },
      ]},
      { no: 6, title: '식별자 명명규칙', slug: '06-식별자-명명규칙', sections: [
        { id: '6.1', title: 'ServiceId (`{BC}.{Domain}.{action}`)', sources: ['znsight-man/명명규칙-07-ServiceId.md', 'znsight-man/부록B-ServiceId-명명규칙.md'] },
        { id: '6.2', title: '거래코드 (`{BC}-{TYPE}-{NNNN}`)', sources: ['znsight-man/명명규칙-08-거래코드.md', 'znsight-man/부록C-거래코드-명명규칙.md'] },
        { id: '6.3', title: 'Header 항목 (JSON·Java·DB·MDC)', sources: ['znsight-man/명명규칙-21-Header-항목.md', 'znsight-man/21-Header-작성-기준.md'] },
        { id: '6.4', title: '화면번호 · menuId · functionCode', sources: ['znsight-man/명명규칙-15-화면번호.md', 'znsight-man/명명규칙-17-화면-ServiceId-연결.md'] },
        { id: '6.5', title: 'Gateway 라우팅·Batch·Cache 명명', sources: ['znsight-man/명명규칙-18-Gateway-라우팅.md', 'znsight-man/명명규칙-19-Batch-Scheduler.md', 'znsight-man/명명규칙-20-Cache.md'] },
      ]},
      { no: 7, title: '코드·DB 명명규칙', slug: '07-코드-DB-명명규칙', sections: [
        { id: '7.1', title: 'Java Class · Method · Field', sources: ['znsight-man/명명규칙-09-Java-Class.md', 'znsight-man/명명규칙-10-Java-Method-Field.md'] },
        { id: '7.2', title: 'DTO 유형·작성 규칙', sources: ['znsight-man/명명규칙-11-Java-DTO.md', 'znsight-man/18-DTO-작성-기준.md'] },
        { id: '7.3', title: 'MyBatis Mapper · SQL ID', sources: ['znsight-man/명명규칙-12-MyBatis-Mapper-SQL.md', 'znsight-man/28-MyBatis-Mapper-개발.md', 'znsight-man/29-SQL-작성-기준.md'] },
        { id: '7.4', title: 'DB 테이블·컬럼·인덱스', sources: ['znsight-man/명명규칙-13-DB-객체.md', 'docs/architecture/19-tcf-table.md'] },
        { id: '7.5', title: '오류코드 · 메시지코드', sources: ['znsight-man/명명규칙-14-오류코드.md', 'znsight-man/부록F-오류코드-표준표.md'] },
        { id: '7.6', title: '로그·감사로그 항목', sources: ['znsight-man/명명규칙-16-로그-감사로그.md', 'znsight-man/34-로그-작성-기준.md', 'znsight-man/35-거래로그-감사로그-기준.md'] },
      ]},
    ],
  },
  {
    dir: '제03편',
    partTitle: '제3편 · 거래 개발 실무',
    chapters: [
      { no: 8, title: '거래 설계 (설계 단계)', slug: '08-거래-설계', sections: [
        { id: '8.1', title: 'ServiceId 설계 · Catalog 등록', sources: ['znsight-man/16-ServiceId-설계.md', 'znsight-man/47-ServiceId-등록-절차.md'] },
        { id: '8.2', title: '거래코드 설계', sources: ['znsight-man/17-거래코드-설계.md'] },
        { id: '8.3', title: 'Header 7항 · 거래통제 설계', sources: ['docs/architecture/39-header-transaction-control.md', 'docs/architecture/40-header-7-transaction-control.md', 'zman/13-거래통제.md'] },
        { id: '8.4', title: 'Timeout 정책 설계', sources: ['docs/architecture/41-service-timeout-policy.md', 'zman/14-Timeout관리.md'] },
        { id: '8.5', title: '화면–ServiceId–거래코드 정합성', sources: ['znsight-man/명명규칙-17-화면-ServiceId-연결.md', 'znsight-man/21-Header-작성-기준.md'] },
      ]},
      { no: 9, title: '표준 전문과 DTO', slug: '09-표준-전문과-DTO', sections: [
        { id: '9.1', title: 'Request / Response 전문 구조', sources: ['znsight-man/20-표준-전문-구조.md', 'znsight-man/부록D-표준-전문-예시.md'] },
        { id: '9.2', title: 'Header 작성·검증·오류코드', sources: ['znsight-man/21-Header-작성-기준.md', 'znsight-man/명명규칙-21-Header-항목.md'] },
        { id: '9.3', title: 'Body DTO · Validation', sources: ['znsight-man/18-DTO-작성-기준.md', 'znsight-man/19-Validation-작성-기준.md'] },
        { id: '9.4', title: 'StandardResponse · ETF 조립', sources: ['docs/architecture/36-ETF.md', 'docs/architecture/04-messaging.md'] },
        { id: '9.5', title: 'Idempotency · 중복 방지', sources: ['znsight-man/38-Idempotency-중복요청.md'] },
      ]},
      { no: 10, title: 'TransactionHandler 개발', slug: '10-TransactionHandler-개발', sections: [
        { id: '10.1', title: 'Handler 등록·serviceIds() 패턴', sources: ['znsight-man/23-TransactionHandler-개발.md', 'zman/08-업무Handler개발.md'] },
        { id: '10.2', title: 'Online Endpoint 호출 규약', sources: ['znsight-man/22-Online-Endpoint-기준.md'] },
        { id: '10.3', title: 'Facade · Service · Rule 구현', sources: ['znsight-man/24-Facade-개발.md', 'znsight-man/25-Service-개발.md', 'znsight-man/26-Rule-개발.md'] },
        { id: '10.4', title: 'DAO · MyBatis Mapper', sources: ['znsight-man/27-DAO-개발.md', 'znsight-man/28-MyBatis-Mapper-개발.md', 'docs/architecture/07-DAO.md', 'docs/architecture/26-mybatis.md'] },
        { id: '10.5', title: 'SQL 작성·페이징', sources: ['znsight-man/29-SQL-작성-기준.md', 'znsight-man/30-페이징-처리-기준.md', 'docs/architecture/27-paging.md'] },
        { id: '10.6', title: '서비스 간 연동 (tcf-eai)', sources: ['znsight-man/31-서비스간-연동-개발.md', 'docs/architecture/46-service-integration-contract.md', 'zguide/tcf-eai-개발가이드.md'] },
      ]},
      { no: 11, title: '품질 속성 구현', slug: '11-품질-속성-구현', sections: [
        { id: '11.1', title: '예외 처리 · BusinessException', sources: ['znsight-man/32-예외처리-기준.md', 'docs/architecture/05-exception.md'] },
        { id: '11.2', title: '오류코드·사용자/운영 메시지', sources: ['znsight-man/33-오류코드-메시지-기준.md'] },
        { id: '11.3', title: '거래로그·감사로그', sources: ['znsight-man/35-거래로그-감사로그-기준.md', 'docs/architecture/37-transaction-log.md'] },
        { id: '11.4', title: '트랜잭션 경계', sources: ['znsight-man/36-트랜잭션-기준.md', 'docs/architecture/03-transaction.md'] },
        { id: '11.5', title: 'Timeout 적용·초과 처리', sources: ['znsight-man/37-Timeout-기준.md', 'docs/architecture/08-timeout.md', 'docs/architecture/41-service-timeout-policy.md'] },
        { id: '11.6', title: 'Cache 사용', sources: ['znsight-man/43-Cache-사용-기준.md', 'docs/architecture/12-cache.md', 'zguide/tcf-cache-개발가이드.md'] },
        { id: '11.7', title: '파일 업·다운로드', sources: ['znsight-man/44-파일-업다운로드-기준.md', 'docs/architecture/18-fileupdownload.md'] },
      ]},
    ],
  },
  {
    dir: '제04편',
    partTitle: '제4편 · 보안·인증·통제',
    chapters: [
      { no: 12, title: '세션·로그인·권한', slug: '12-세션-로그인-권한', sections: [
        { id: '12.1', title: 'Spring Session · SESSIONDB', sources: ['docs/architecture/10-session.md', 'zman/10-세션관리.md'] },
        { id: '12.2', title: '로그인·사용자 정보', sources: ['docs/architecture/11-login.md', 'docs/설계자료/README.md'] },
        { id: '12.3', title: '기능권한·메뉴', sources: ['docs/설계자료/README.md'] },
        { id: '12.4', title: '세션 사용 기준 (업무 WAR)', sources: ['znsight-man/39-세션-사용-기준.md'] },
        { id: '12.5', title: '권한 검증 기준', sources: ['znsight-man/40-권한-검증-기준.md'] },
      ]},
      { no: 13, title: 'JWT · SSO · Gateway', slug: '13-JWT-SSO-Gateway', sections: [
        { id: '13.1', title: 'JWT 발급·갱신·폐기·JWKS', sources: ['docs/architecture/42-jwt.md', 'zguide/tcf-jwt-개발가이드.md'] },
        { id: '13.2', title: 'tcf-web JWT Filter', sources: ['znsight-man/80-JWT-TCF-WEB-개발-매뉴얼.md'] },
        { id: '13.3', title: 'Gateway JWT·라우팅', sources: ['docs/architecture/51-api-gateway.md', 'znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md', 'zguide/tcf-gateway-개발가이드.md'] },
        { id: '13.4', title: 'JWT/SSO 연계 기준', sources: ['znsight-man/41-JWT-SSO-연계.md'] },
        { id: '13.5', title: '보안 코딩·보안 운영', sources: ['znsight-man/42-보안-코딩-기준.md', 'docs/architecture/43-security-operations.md'] },
      ]},
      { no: 14, title: '거래통제·정책', slug: '14-거래통제-정책', sections: [
        { id: '14.1', title: 'Header 7항 Allow-List', sources: ['docs/architecture/40-header-7-transaction-control.md', 'zman/13-거래통제.md'] },
        { id: '14.2', title: 'businessCode · URL · Prefix 정합성', sources: ['znsight-man/명명규칙-21-Header-항목.md'] },
        { id: '14.3', title: 'OM 거래통제 등록 절차', sources: ['znsight-man/48-거래통제-등록-절차.md'] },
        { id: '14.4', title: 'Timeout·ServiceId Catalog OM 등록', sources: ['znsight-man/47-ServiceId-등록-절차.md', 'znsight-man/49-Timeout-정책-등록.md'] },
        { id: '14.5', title: '공통코드·오류코드 OM 등록', sources: ['znsight-man/50-공통코드-사용-절차.md', 'znsight-man/51-오류코드-등록-절차.md'] },
      ]},
    ],
  },
  {
    dir: '제05편',
    partTitle: '제5편 · 플랫폼·운영 관리 (OM)',
    chapters: [
      { no: 15, title: 'OM 아키텍처와 개발', slug: '15-OM-아키텍처와-개발', sections: [
        { id: '15.1', title: 'tcf-om vs om-service (레거시)', sources: ['zarchitecture/05-운영관리-OM-아키텍처.md', 'zguide/tcf-om-개발가이드.md'] },
        { id: '15.2', title: 'OM Handler·화면·serviceId', sources: ['docs/architecture/52-om-operations.md', 'zman/12-OM운영관리.md'] },
        { id: '15.3', title: 'Service Catalog · 거래통제 · Timeout', sources: ['znsight-man/46-OM-운영관리-개발.md', 'znsight-man/47-ServiceId-등록-절차.md', 'znsight-man/48-거래통제-등록-절차.md', 'znsight-man/49-Timeout-정책-등록.md'] },
        { id: '15.4', title: '환경설정 조회·배포관리', sources: ['znsight-man/52-배포관리-연계.md', 'znsight-man/53-환경설정-조회-연계.md'] },
        { id: '15.5', title: '운영 대시보드·헬스체크', sources: ['docs/architecture/44-observability.md', 'docs/설계자료/README.md'] },
      ]},
      { no: 16, title: 'API Gateway · UI 채널', slug: '16-API-Gateway-UI-채널', sections: [
        { id: '16.1', title: 'Gateway STF/GRF/GSF/GEF', sources: ['docs/architecture/51-api-gateway.md', 'zarchitecture/06-API-Gateway-아키텍처.md'] },
        { id: '16.2', title: 'Apache·Spring 라우팅', sources: ['docs/architecture/23-env-apache.md', 'zman/09-Gateway라우팅.md'] },
        { id: '16.3', title: 'tcf-ui · tcf-uj (Relay·Gateway UI)', sources: ['zarchitecture/13-UI-채널-아키텍처.md', 'zguide/tcf-ui-개발가이드.md', 'zguide/tcf-uj-개발가이드.md'] },
        { id: '16.4', title: '채널 ID · 외부 REST 연계', sources: ['docs/architecture/14-online-arc.md', 'docs/architecture/46-service-integration-contract.md'] },
      ]},
      { no: 17, title: 'Batch · Scheduler · 이벤트', slug: '17-Batch-Scheduler-이벤트', sections: [
        { id: '17.1', title: 'Batch/Scheduler 아키텍처', sources: ['docs/architecture/13-batch.md', 'docs/architecture/15-schedule.md', 'zarchitecture/12-배치-모니터링-아키텍처.md'] },
        { id: '17.2', title: 'Batch 개발 기준', sources: ['znsight-man/45-Batch-Scheduler-개발.md'] },
        { id: '17.3', title: 'tcf-batch 모니터링 수집', sources: ['zguide/tcf-batch-개발가이드.md'] },
        { id: '17.4', title: '이벤트 연계 (EB/EP)', sources: ['zarchitecture/14-이벤트-연계-아키텍처.md', 'zguide/eb-service-개발가이드.md', 'zguide/ep-service-개발가이드.md'] },
      ]},
      { no: 18, title: '데이터·DB 아키텍처', slug: '18-데이터-DB-아키텍처', sections: [
        { id: '18.1', title: 'RDW · ADW · OMDB · LOGDB · SESSIONDB', sources: ['zarchitecture/09-데이터-DB-아키텍처.md', 'zman/19-DB-테이블.md'] },
        { id: '18.2', title: 'TCF 핵심 테이블', sources: ['docs/architecture/19-tcf-table.md'] },
        { id: '18.3', title: 'MyBatis·DAO 패턴', sources: ['docs/architecture/07-DAO.md', 'docs/architecture/26-mybatis.md'] },
        { id: '18.4', title: '데이터 거버넌스·마스킹', sources: ['docs/architecture/47-data-governance.md'] },
      ]},
    ],
  },
  {
    dir: '제06편',
    partTitle: '제6편 · 환경·빌드·배포',
    chapters: [
      { no: 19, title: '로컬 개발환경', slug: '19-로컬-개발환경', sections: [
        { id: '19.1', title: 'JDK·Gradle·DB·IDE 구성', sources: ['znsight-man/06-로컬-개발환경-구성.md'] },
        { id: '19.2', title: 'application.yml·Profile', sources: ['znsight-man/11-application-yml-기준.md', 'docs/architecture/20-env-spring.md', 'docs/architecture/25-env-profile.md', 'znsight-man/부록G-application-yml-템플릿.md'] },
        { id: '19.3', title: 'Spring/Tomcat/Apache 환경', sources: ['docs/architecture/20-env-spring.md', 'docs/architecture/21-env-tomcat.md', 'docs/architecture/23-env-apache.md', 'docs/architecture/24-env-spring-detail.md', 'zman/20-Spring환경설정.md'] },
        { id: '19.4', title: '로컬 빌드·bootRun', sources: ['znsight-man/63-로컬-빌드-방법.md', 'zguide/tcf-scripts-개발가이드.md'] },
        { id: '19.5', title: 'ztomcat 8080 통합 검증', sources: ['ztomcat/README.md', 'znsight-man/10-bootRun-Tomcat-WAR-차이.md'] },
      ]},
      { no: 20, title: 'CI/CD · 릴리즈 · DR', slug: '20-CICD-릴리즈-DR', sections: [
        { id: '20.1', title: 'WAR 생성·배포 절차', sources: ['znsight-man/64-WAR-생성-기준.md', 'znsight-man/65-CICD-파이프라인-기준.md', 'znsight-man/66-배포-절차.md', 'docs/architecture/16-deploy.md'] },
        { id: '20.2', title: 'CI/CD 파이프라인', sources: ['znsight-man/65-CICD-파이프라인-기준.md', 'docs/architecture/49-release-strategy.md', 'zguide/tcf-cicd-개발가이드.md'] },
        { id: '20.3', title: '롤백·운영 전환', sources: ['znsight-man/67-롤백-절차.md', 'znsight-man/68-운영-전환-체크리스트.md', 'docs/architecture/45-disaster-recovery.md'] },
        { id: '20.4', title: '릴리즈·브랜치 전략', sources: ['docs/architecture/49-release-strategy.md', 'znsight-man/07-Git-브랜치-기준.md'] },
        { id: '20.5', title: '장애 대응·FAQ', sources: ['znsight-man/69-장애-개발자-확인-항목.md', 'znsight-man/70-FAQ-Troubleshooting.md'] },
      ]},
    ],
  },
  {
    dir: '제07편',
    partTitle: '제7편 · 테스트·품질 보증',
    chapters: [
      { no: 21, title: '테스트 전략', slug: '21-테스트-전략', sections: [
        { id: '21.1', title: '테스트 아키텍처 개요', sources: ['docs/architecture/50-test-architecture.md'] },
        { id: '21.2', title: '단위·통합·TCF 거래 테스트', sources: ['znsight-man/54-단위-테스트-기준.md', 'znsight-man/55-통합-테스트-기준.md', 'znsight-man/56-TCF-거래-테스트-기준.md'] },
        { id: '21.3', title: 'MyBatis SQL·보안·성능·장애 테스트', sources: ['znsight-man/57-MyBatis-SQL-테스트-기준.md', 'znsight-man/58-보안-테스트-기준.md', 'znsight-man/59-성능-테스트-기준.md', 'znsight-man/60-장애-테스트-기준.md'] },
        { id: '21.4', title: '코드 리뷰·품질 게이트', sources: ['znsight-man/61-코드-리뷰-기준.md', 'znsight-man/62-품질-게이트-기준.md', 'znsight-man/부록I-코드-리뷰-체크리스트.md'] },
        { id: '21.5', title: '개발 완료·운영 전환 체크리스트', sources: ['znsight-man/부록H-개발-완료-체크리스트.md', 'znsight-man/부록J-운영-전환-체크리스트.md'] },
      ]},
    ],
  },
  {
    dir: '제08편',
    partTitle: '제8편 · 실습 — End-to-End 샘플',
    chapters: [
      { no: 22, title: '조회 거래 (SV 고객요약)', slug: '22-조회-거래-SV-고객요약', sections: [
        { id: '22.1', title: '요건·ServiceId·거래코드', sources: ['znsight-man/71-SV-고객요약조회-샘플.md', 'zman/22-업무서비스샘플.md'] },
        { id: '22.2', title: 'Handler~Mapper 구현', sources: ['zguide/sv-service-개발가이드.md'] },
        { id: '22.3', title: 'OM 등록·거래 테스트', sources: ['znsight-man/47-ServiceId-등록-절차.md', 'znsight-man/56-TCF-거래-테스트-기준.md'] },
      ]},
      { no: 23, title: '목록·페이징·등록·변경', slug: '23-목록-페이징-등록-변경', sections: [
        { id: '23.1', title: '목록조회 + 페이징', sources: ['znsight-man/72-목록조회-페이징-샘플.md'] },
        { id: '23.2', title: '등록/변경 거래', sources: ['znsight-man/73-등록변경-거래-샘플.md'] },
        { id: '23.3', title: '외부 서비스 호출 (EAI)', sources: ['znsight-man/74-외부-서비스-호출-샘플.md'] },
        { id: '23.4', title: '파일 다운로드', sources: ['znsight-man/75-파일-다운로드-샘플.md'] },
        { id: '23.5', title: 'Batch Job', sources: ['znsight-man/76-Batch-Job-샘플.md'] },
        { id: '23.6', title: '오류처리·테스트 코드', sources: ['znsight-man/77-오류처리-샘플.md', 'znsight-man/78-테스트-코드-샘플.md'] },
      ]},
    ],
  },
  {
    dir: '제09편',
    partTitle: '제9편 · 모듈별 레퍼런스 (Quick Start)',
    chapters: [
      { no: 24, title: 'tcf-core · tcf-web · tcf-util', slug: '24-tcf-core-web-util', sections: [
        { id: '24.1', title: '공통 라이브러리 개요', sources: ['zguide/tcf-core-개발가이드.md', 'docs/architecture/28-tcf-framework-ref.md', 'docs/architecture/33-TCF.md', 'tcf-util/README.md', 'tcf-web/README.md'] },
      ]},
      { no: 25, title: 'tcf-om · tcf-ui · tcf-uj', slug: '25-tcf-om-ui-uj', sections: [
        { id: '25.1', title: '운영·UI 모듈 Quick Start', sources: ['zguide/tcf-om-개발가이드.md', 'zguide/tcf-ui-개발가이드.md', 'zguide/tcf-uj-개발가이드.md'] },
      ]},
      { no: 26, title: 'tcf-gateway · tcf-jwt', slug: '26-tcf-gateway-jwt', sections: [
        { id: '26.1', title: 'Gateway·JWT 모듈 Quick Start', sources: ['zguide/tcf-gateway-개발가이드.md', 'zguide/tcf-jwt-개발가이드.md', 'docs/architecture/42-jwt.md', 'docs/architecture/51-api-gateway.md'] },
      ]},
      { no: 27, title: 'tcf-eai · tcf-cache · tcf-batch', slug: '27-tcf-eai-cache-batch', sections: [
        { id: '27.1', title: '연동·캐시·배치 모듈 Quick Start', sources: ['zguide/tcf-eai-개발가이드.md', 'zguide/tcf-cache-개발가이드.md', 'zguide/tcf-batch-개발가이드.md'] },
      ]},
      { no: 28, title: 'tcf-cicd · tcf-scripts', slug: '28-tcf-cicd-scripts', sections: [
        { id: '28.1', title: 'CI/CD·스크립트 Quick Start', sources: ['zguide/tcf-cicd-개발가이드.md', 'zguide/tcf-scripts-개발가이드.md'] },
      ]},
      { no: 29, title: 'ic · pc · ms · sv · pd (업무 WAR 5)', slug: '29-업무-WAR-ic-pc-ms-sv-pd', sections: [
        { id: '29.1', title: '업무 WAR 5종 Quick Start', sources: ['zguide/ic-service-개발가이드.md', 'zguide/pc-service-개발가이드.md', 'zguide/ms-service-개발가이드.md', 'zguide/sv-service-개발가이드.md', 'zguide/pd-service-개발가이드.md'] },
      ]},
      { no: 30, title: 'eb · ep · ss · mg (업무 WAR 4)', slug: '30-업무-WAR-eb-ep-ss-mg', sections: [
        { id: '30.1', title: '업무 WAR 4종 Quick Start', sources: ['zguide/eb-service-개발가이드.md', 'zguide/ep-service-개발가이드.md', 'zguide/ss-service-개발가이드.md', 'zguide/mg-service-개발가이드.md'] },
      ]},
    ],
  },
  {
    dir: '제10편',
    partTitle: '제10편 · 설계 근거와 로드맵',
    chapters: [
      { no: 31, title: '공식 설계안 매핑', slug: '31-공식-설계안-매핑', sections: [
        { id: '31.1', title: '20+ Word 설계안 목록·주제', sources: ['docs/설계자료/README.md'] },
        { id: '31.2', title: '설계안 ↔ 코드 ↔ OM 화면', sources: ['docs/설계자료/README.md'] },
        { id: '31.3', title: 'ADR·최종 아키텍처 결정', sources: ['docs/architecture/NSIGHT-FINAL-ARCHITECTURE-DECISION.md'] },
      ]},
      { no: 32, title: 'Gap·보완·향후 과제', slug: '32-Gap-보완-향후-과제', sections: [
        { id: '32.1', title: '설계서 vs 코드 Gap', sources: ['zman/23-소스Gap분석.md'] },
        { id: '32.2', title: '보완 우선순위', sources: ['zman/24-보완과제-우선순위.md'] },
        { id: '32.3', title: '17업무 WAR 확장·관측성·DR', sources: ['docs/architecture/44-observability.md', 'docs/architecture/45-disaster-recovery.md', 'docs/architecture/architecture.md'] },
      ]},
    ],
  },
];

const APPENDIX = [
  { id: 'A', title: '업무코드 표준표', slug: 'A-업무코드-표준표', sources: ['znsight-man/부록A-업무코드-표준표.md'] },
  { id: 'B', title: 'ServiceId 명명규칙 요약', slug: 'B-ServiceId-명명규칙', sources: ['znsight-man/부록B-ServiceId-명명규칙.md'] },
  { id: 'C', title: '거래코드 명명규칙 요약', slug: 'C-거래코드-명명규칙', sources: ['znsight-man/부록C-거래코드-명명규칙.md'] },
  { id: 'D', title: '표준 전문 JSON 예시', slug: 'D-표준-전문-JSON-예시', sources: ['znsight-man/부록D-표준-전문-예시.md'] },
  { id: 'E', title: 'Mapper XML 템플릿', slug: 'E-Mapper-XML-템플릿', sources: ['znsight-man/부록E-Mapper-XML-템플릿.md'] },
  { id: 'F', title: '오류코드 표준표', slug: 'F-오류코드-표준표', sources: ['znsight-man/부록F-오류코드-표준표.md'] },
  { id: 'G', title: 'application.yml 템플릿', slug: 'G-application-yml-템플릿', sources: ['znsight-man/부록G-application-yml-템플릿.md'] },
  { id: 'H', title: '개발 완료 체크리스트', slug: 'H-개발-완료-체크리스트', sources: ['znsight-man/부록H-개발-완료-체크리스트.md'] },
  { id: 'I', title: '코드 리뷰 체크리스트', slug: 'I-코드-리뷰-체크리스트', sources: ['znsight-man/부록I-코드-리뷰-체크리스트.md'] },
  { id: 'J', title: '운영 전환 체크리스트', slug: 'J-운영-전환-체크리스트', sources: ['znsight-man/부록J-운영-전환-체크리스트.md'] },
  { id: 'K', title: '모듈·포트·Context·WAR 매핑표', slug: 'K-모듈-포트-Context-WAR-매핑표', sources: ['zarchitecture/16-모듈-포트-의존성-레퍼런스.md'] },
  { id: 'L', title: 'TCF 핵심 테이블 DDL 요약', slug: 'L-TCF-핵심-테이블-DDL-요약', sources: ['docs/architecture/19-tcf-table.md'] },
  { id: 'M', title: '명명규칙 21주제 전체 색인', slug: 'M-명명규칙-21주제-색인', sources: ['znsight-man/명명규칙-00-목차.md'] },
  { id: 'N', title: '소스 인덱스 (클래스·패키지)', slug: 'N-소스-인덱스', sources: ['docs/SOURCE_INDEX.md'] },
];

function repoLink(relFromBook, repoPath) {
  return `../../${repoPath.replace(/^\//, '')}`;
}

function chapterHeading(no, title) {
  if (no === 0) return '서문';
  return `제${no}장. ${title}`;
}

function renderSection(sec, depth) {
  const lines = [
    `## ${sec.id} ${sec.title}`,
    '',
    '<!-- 집필: 아래 출처를 통합·편집하여 작성 -->',
    '',
    '### 참고 출처',
    '',
    ...sec.sources.map((s) => `- [${s}](${repoLink('', s)})`),
    '',
  ];
  return lines.join('\n');
}

function renderChapter(part, ch, prev, next) {
  const heading = chapterHeading(ch.no, ch.title);
  const fileName = `${ch.slug}.md`;
  const prevLink = prev
    ? `[${prev.label}](../${prev.dir}/${prev.slug}.md)`
    : '[00-목차](../00-목차.md)';
  const nextLink = next
    ? `[${next.label}](../${next.dir}/${next.slug}.md)`
    : '—';

  const body = ch.sections.map((s) => renderSection(s)).join('\n');

  return `# ${heading}

| 항목 | 내용 |
| --- | --- |
| **편** | ${part.partTitle} |
| **장** | ${ch.no === 0 ? '서문' : `제${ch.no}장`} |
| **파일** | \`${part.dir}/${fileName}\` |
| **상태** | 집필 대기 |
| **목차** | [00-목차](../00-목차.md) |

---

${body}
## 장 요약

<!-- TODO: 핵심 3~5문장 -->

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | ${prevLink} |
| → 다음 | ${nextLink} |
`;
}

function renderAppendix(app, prev, next) {
  const prevLink = prev ? `[부록 ${prev.id}](./${prev.slug}.md)` : '[32-Gap-보완-향후-과제](../제10편/32-Gap-보완-향후-과제.md)';
  const nextLink = next ? `[부록 ${next.id}](./${next.slug}.md)` : '—';

  return `# 부록 ${app.id}. ${app.title}

| 항목 | 내용 |
| --- | --- |
| **부록** | ${app.id} |
| **상태** | 집필 대기 (출처 문서 재편집) |
| **목차** | [00-목차](../00-목차.md) |

---

<!-- 집필: 아래 출처를 통합·편집하여 작성 -->

## 참고 출처

${app.sources.map((s) => `- [${s}](${repoLink('', s)})`).join('\n')}

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | ${prevLink} |
| → 다음 | ${nextLink} |
`;
}

function renderPartReadme(part) {
  const rows = part.chapters.map((ch) => {
    const label = ch.no === 0 ? '서문' : `제${ch.no}장`;
    return `| ${label} | [${ch.title}](./${ch.slug}.md) | 집필 대기 |`;
  });
  return `# ${part.partTitle}

| 장 | 제목 | 상태 |
| --- | --- | --- |
${rows.join('\n')}

← [전체 목차](../00-목차.md)
`;
}

function renderAppendixReadme() {
  const rows = APPENDIX.map((a) => `| ${a.id} | [${a.title}](./${a.slug}.md) | 집필 대기 |`);
  return `# 부록

| 부록 | 제목 | 상태 |
| --- | --- | --- |
${rows.join('\n')}

← [전체 목차](../00-목차.md)
`;
}

// Flat nav list
const nav = [];
for (const part of BOOK) {
  for (const ch of part.chapters) {
    nav.push({
      dir: part.dir,
      slug: ch.slug,
      label: ch.no === 0 ? '서문' : `제${ch.no}장 ${ch.title}`,
    });
  }
}

let count = 0;

for (let pi = 0; pi < BOOK.length; pi++) {
  const part = BOOK[pi];
  const partDir = path.join(root, part.dir);
  fs.mkdirSync(partDir, { recursive: true });
  fs.writeFileSync(path.join(partDir, 'README.md'), renderPartReadme(part), 'utf8');

  for (let ci = 0; ci < part.chapters.length; ci++) {
    const ch = part.chapters[ci];
    const globalIdx = nav.findIndex((n) => n.dir === part.dir && n.slug === ch.slug);
    const prev = globalIdx > 0 ? nav[globalIdx - 1] : null;
    const next = globalIdx < nav.length - 1 ? nav[globalIdx + 1] : null;
    const content = renderChapter(part, ch, prev, next);
    const out = path.join(partDir, `${ch.slug}.md`);
    fs.writeFileSync(out, content, 'utf8');
    count++;
    console.log('Wrote', path.relative(root, out));
  }
}

const appendixDir = path.join(root, '부록');
fs.mkdirSync(appendixDir, { recursive: true });
fs.writeFileSync(path.join(appendixDir, 'README.md'), renderAppendixReadme(), 'utf8');

for (let i = 0; i < APPENDIX.length; i++) {
  const app = APPENDIX[i];
  const prev = i > 0 ? APPENDIX[i - 1] : null;
  const next = i < APPENDIX.length - 1 ? APPENDIX[i + 1] : null;
  const out = path.join(appendixDir, `${app.slug}.md`);
  fs.writeFileSync(out, renderAppendix(app, prev, next), 'utf8');
  count++;
  console.log('Wrote', path.relative(root, out));
}

// Remove placeholder if exists
const placeholder = path.join(root, '제01편', 'xx장');
if (fs.existsSync(placeholder)) {
  fs.unlinkSync(placeholder);
  console.log('Removed placeholder xx장');
}

console.log(`\nDone: ${count} chapter/appendix files in ${BOOK.length + 1} directories.`);
