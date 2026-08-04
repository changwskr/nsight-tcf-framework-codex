아래 표는 앞서 정리한 내용을 **영역별 아키텍처 의사결정 관리대장** 형태로 재구성한 것입니다. 각 항목은 이후 개별 방안서와 ADR의 작성 단위로 사용할 수 있습니다. NSIGHT의 의사결정은 문서 승인으로 끝내지 않고 코드·설정·OM 기준정보·테스트·운영 증적까지 연결하는 것을 완료 기준으로 삼아야 합니다.

# 농협 상호금융 NSIGHT

# 개발단계 영역별 아키텍처 의사결정 사항 목록

## 1. 관리 컬럼 정의

| 컬럼                   | 의미                                                   |
| ---------------------- | ------------------------------------------------------ |
| ID                     | 아키텍처 의사결정 고유번호                             |
| 아키텍처 의사결정 사항 | 프로젝트에서 공식적으로 확정할 대상                    |
| 주요 결정내용          | 방안서에서 반드시 결정해야 하는 내용                   |
| 주관                   | 방안 작성과 이행을 책임지는 조직                       |
| 협의                   | 요구사항과 영향도를 검토하는 조직                      |
| 승인                   | 최종 결정권자                                          |
| 우선순위               | P0 즉시, P1 본 개발 전, P2 통합시험 전, P3 운영 고도화 |
| 주요 산출물            | 의사결정 완료를 증명하는 결과물                        |

---

# 2. 거버넌스·아키텍처 관리

| ID     | 아키텍처 의사결정 사항    | 주요 결정내용                  | 주관 | 협의         | 승인            | 우선 | 주요 산출물        |
| ------ | ------------------------- | ------------------------------ | ---- | ------------ | --------------- | ---: | ------------------ |
| GOV-01 | 아키텍처 의사결정 절차    | 등록·검토·승인·변경·폐기 절차  | SA   | 전 아키텍처  | 프로젝트 책임자 |   P0 | ADR 관리기준       |
| GOV-02 | 아키텍처 RACI             | 영역별 작성·협의·승인 책임     | SA   | PMO          | 프로젝트 책임자 |   P0 | RACI 매트릭스      |
| GOV-03 | Architecture Review Board | 참석자·의결권·회의주기·정족수  | SA   | PMO·QA       | 프로젝트 책임자 |   P0 | ARB 운영규정       |
| GOV-04 | 공식 기준문서 지정        | 개발·설계 기준의 단일 기준원   | SA   | AA·DA·TA·SEC | ARB             |   P0 | 기준문서 목록      |
| GOV-05 | ADR 번호체계              | 영역·순번·상태·대체관계        | SA   | PMO          | ARB             |   P0 | ADR 템플릿         |
| GOV-06 | 아키텍처 예외 승인        | 예외 사유·보완책·만료일·책임자 | SA   | QA·PMO       | ARB             |   P0 | 예외승인서         |
| GOV-07 | 기술부채 관리             | 임시 구현·제거일·이행책임      | AA   | SA·PMO       | ARB             |   P1 | 기술부채 원장      |
| GOV-08 | Risk·Gap 관리             | 위험등급·조치기한·종료기준     | SA   | 전 영역      | ARB             |   P0 | Risk·Gap 원장      |
| GOV-09 | 개발 완료 기준            | 설계·코드·설정·시험·운영 증적  | QA   | AA·DVO·OPS   | SA              |   P0 | Definition of Done |
| GOV-10 | 아키텍처 Gate             | 설계·개발·시험·전환 단계 Gate  | SA   | QA·PMO       | ARB             |   P0 | Gate 체크리스트    |
| GOV-11 | 공통 샘플 기준            | 기준 거래·정상 구현·참조 경로  | FW   | AA·DEV       | SA              |   P0 | Golden Sample      |
| GOV-12 | 공통 변경 전파            | 영향분석·공지·적용기한·호환성  | SA   | 전 영역      | ARB             |   P1 | 변경영향 보고서    |

---

# 3. 업무·도메인·애플리케이션 아키텍처

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                          | 주관 | 협의      | 승인 | 우선 | 주요 산출물        |
| ------ | ---------------------- | -------------------------------------- | ---- | --------- | ---- | ---: | ------------------ |
| APP-01 | 업무 도메인 분할       | 도메인 경계·책임·변경주체              | AA   | BA·DA     | SA   |   P0 | 도메인 목록        |
| APP-02 | 업무코드 체계          | 코드·업무명·WAR·소유조직               | SA   | AA·BA     | ARB  |   P0 | 업무코드 관리대장  |
| APP-03 | 업무 WAR 구성          | 업무코드와 WAR 배포단위 관계           | AA   | TA·DVO    | SA   |   P0 | WAR 구성표         |
| APP-04 | 애플리케이션 계층      | Handler–Facade–Service–Rule–DAO–Mapper | AA   | FW·DEV    | SA   |   P0 | 계층 설계기준      |
| APP-05 | 공통 Controller 적용   | 업무별 Controller 허용 여부            | FW   | AA·SEC    | SA   |   P0 | Endpoint 설계서    |
| APP-06 | TCF 진입 강제          | TCF 우회 거래 차단 방식                | FW   | AA·QA     | SA   |   P0 | TCF 적용기준       |
| APP-07 | Handler 책임           | ServiceId 분기·Facade 호출 범위        | FW   | AA·DEV    | AA   |   P0 | Handler 표준       |
| APP-08 | Facade 책임            | 유스케이스 조립·트랜잭션 경계          | AA   | FW·DEV    | SA   |   P0 | Facade 표준        |
| APP-09 | Service 책임           | 업무 흐름·도메인 기능·외부 호출        | AA   | BA·DEV    | SA   |   P0 | Service 표준       |
| APP-10 | Rule 책임              | 부작용 없는 업무규칙 분리 기준         | AA   | BA·DEV    | SA   |   P1 | Rule 표준          |
| APP-11 | DAO·Mapper 책임        | SQL 실행과 업무로직 분리               | DA   | DBA·AA    | SA   |   P0 | DAO·Mapper 기준    |
| APP-12 | DTO 분리               | Request·Response·Command·Query·Result  | AA   | UI·DA·FW  | SA   |   P0 | DTO 설계기준       |
| APP-13 | 트랜잭션 경계          | 시작·종료·Rollback 위치                | AA   | FW·DA     | SA   |   P0 | 트랜잭션 설계서    |
| APP-14 | 읽기 전용 거래         | readOnly 적용대상과 예외               | AA   | DA·DBA    | SA   |   P1 | 조회거래 기준      |
| APP-15 | 도메인 간 호출         | 동일 WAR 공개계약과 직접 호출 제한     | AA   | FW·DA     | SA   |   P0 | 도메인 연동기준    |
| APP-16 | WAR 간 호출            | Java 직접참조 금지와 ServiceId 호출    | AA   | EAI·FW    | SA   |   P0 | WAR 연계기준       |
| APP-17 | 공통 Util 기준         | 공통화 조건·업무로직 유입 방지         | FW   | AA·DEV    | AA   |   P1 | 공통화 심의기준    |
| APP-18 | 비동기 처리            | Executor·Context·오류·종료 관리        | FW   | AA·TA·OPS | SA   |   P1 | 비동기 처리기준    |
| APP-19 | API 예외 Endpoint      | Health·파일·인증 등 TCF 비경유 대상    | AA   | FW·SEC    | SA   |   P0 | 예외 Endpoint 목록 |
| APP-20 | 업무 프로그램 추적성   | 화면–ServiceId–프로그램–SQL 연결       | AA   | UI·DA·QA  | SA   |   P1 | 추적성 매트릭스    |

NSIGHT의 업무 프로그램은 공통 진입점에서 `TCF → STF → Dispatcher → Handler → Facade → Service → Rule·DAO → Mapper → ETF` 순서로 처리하는 구조를 기준으로 합니다.

---

# 4. 명명·식별자·추적성

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                      | 주관 | 협의        | 승인 | 우선 | 주요 산출물      |
| ------ | ---------------------- | ---------------------------------- | ---- | ----------- | ---- | ---: | ---------------- |
| STD-01 | 화면 ID                | 업무·세부업무·일련번호 형식        | UI   | BA·AA       | AA   |   P0 | 화면 ID 규칙     |
| STD-02 | 화면 이벤트 ID         | 화면과 이벤트의 관계·채번방법      | UI   | BA·FW       | AA   |   P0 | 이벤트 ID 규칙   |
| STD-03 | ServiceId              | 업무코드·도메인·처리행위 형식      | FW   | AA·OM       | AA   |   P0 | ServiceId 규칙   |
| STD-04 | 거래코드               | 운영통제·통계용 거래 식별체계      | FW   | AA·OPS      | AA   |   P0 | 거래코드 규칙    |
| STD-05 | Java BASE 패키지       | 기관·시스템·플랫폼·업무 구분       | AA   | FW          | SA   |   P0 | BASE 패키지 기준 |
| STD-06 | 업무 패키지            | 업무코드→도메인→계층               | AA   | FW·DEV      | SA   |   P0 | 패키지 구조표    |
| STD-07 | 클래스 명명            | 책임·업무·유형 표현방식            | AA   | FW·QA       | AA   |   P0 | 클래스 명명규칙  |
| STD-08 | 메서드 명명            | 조회·등록·변경·삭제 행위표현       | AA   | DEV·QA      | AA   |   P0 | 메서드 명명규칙  |
| STD-09 | DTO 명명               | Request·Response·Query·Result 구분 | AA   | UI·DA       | AA   |   P0 | DTO 명명규칙     |
| STD-10 | Mapper·SQL ID          | Mapper Namespace·Statement ID      | DA   | DBA·AA      | DA   |   P0 | SQL ID 규칙      |
| STD-11 | DB 객체 명명           | Table·Column·Index·Constraint      | DA   | DBA·AA      | SA   |   P0 | DB 명명규칙      |
| STD-12 | 환경설정 Key           | Prefix·계층·환경별 Key 형식        | FW   | TA·DVO      | AA   |   P1 | 설정 Key 기준    |
| STD-13 | 배치·파일 ID           | Job·Step·File·Interface 식별체계   | AA   | EAI·OPS     | SA   |   P1 | 배치·파일 규칙   |
| STD-14 | GUID·TraceId           | 생성주체·전파구간·로그반영         | FW   | MCA·EAI·OPS | AA   |   P0 | 추적 ID 기준     |
| STD-15 | 명명 자동검증          | Checkstyle·ArchUnit·정규식 검사    | DVO  | AA·DA·FW    | AA   |   P1 | CI 검사규칙      |

---

# 5. 단말·UI 아키텍처

| ID    | 아키텍처 의사결정 사항 | 주요 결정내용                     | 주관 | 협의       | 승인 | 우선 | 주요 산출물      |
| ----- | ---------------------- | --------------------------------- | ---- | ---------- | ---- | ---: | ---------------- |
| UI-01 | 단말 유형별 표준       | WEBTOPSUITE·React·일반 Web 기준   | UI   | TA·SEC·AA  | SA   |   P0 | 단말 적용표      |
| UI-02 | 화면 공통 컴포넌트     | 조회조건·그리드·팝업·버튼 표준    | UI   | BA·QA      | AA   |   P0 | UI 컴포넌트 표준 |
| UI-03 | 입력값 검증            | UI 1차 검증과 서버 필수검증 경계  | UI   | AA·FW      | AA   |   P0 | Validation 기준  |
| UI-04 | 오류 표시              | 오류코드·메시지·사용자 행동       | UI   | BA·FW      | AA   |   P0 | 오류표시 기준    |
| UI-05 | 중복 클릭 방지         | 버튼 차단·요청 식별·멱등성 연계   | UI   | FW·AA      | AA   |   P0 | 중복요청 기준    |
| UI-06 | 화면 상태관리          | 임시상태·서버상태·새로고침 처리   | UI   | BA·AA      | AA   |   P1 | 상태관리 기준    |
| UI-07 | JWT 저장               | 메모리·sessionStorage·Cookie 적용 | UI   | SEC·AA     | SEC  |   P0 | Token 저장기준   |
| UI-08 | iframe 연계            | postMessage·origin·토큰 전달      | UI   | SEC·MCA    | SEC  |   P0 | iframe 보안설계  |
| UI-09 | 브라우저 호환성        | 지원 브라우저·버전·해상도         | UI   | QA·TA      | AA   |   P1 | 호환성 Matrix    |
| UI-10 | 파일 업로드            | 확장자·용량·악성파일·권한         | UI   | AA·SEC·TA  | SEC  |   P1 | 업로드 기준      |
| UI-11 | 파일 다운로드          | 권한·마스킹·감사·대용량 처리      | UI   | AA·SEC·OPS | SEC  |   P0 | 다운로드 기준    |
| UI-12 | 엑셀 처리              | 건수 제한·마스킹·비동기 생성      | UI   | AA·DA·OPS  | AA   |   P1 | 엑셀 처리기준    |
| UI-13 | 접근성·사용성          | 키보드·색상·메시지·업무 흐름      | UI   | BA·QA      | AA   |   P2 | UI 품질기준      |
| UI-14 | 화면 성능              | 초기 로딩·그리드·페이징 기준      | UI   | TA·AA      | AA   |   P1 | 화면 성능기준    |

---

# 6. 개인정보·보안·암호화

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                   | 주관 | 협의        | 승인 | 우선 | 주요 산출물        |
| ------ | ---------------------- | ------------------------------- | ---- | ----------- | ---- | ---: | ------------------ |
| SEC-01 | 개인정보 분류          | 공개·내부·개인·민감·고유식별    | DA   | SEC·BA      | SEC  |   P0 | 개인정보 분류표    |
| SEC-02 | 서버 응답 마스킹       | 권한별 원문·부분마스킹·필드제거 | AA   | SEC·DA·FW   | SEC  |   P0 | 서버 마스킹 방안   |
| SEC-03 | 화면 표시 마스킹       | 화면표시·복사·Tooltip 처리      | UI   | SEC·AA      | SEC  |   P0 | UI 마스킹 기준     |
| SEC-04 | 로그 마스킹            | 개인정보·Token·전문 원문 통제   | FW   | SEC·OPS     | SEC  |   P0 | 로그 마스킹 기준   |
| SEC-05 | 엑셀·파일 마스킹       | 화면과 다운로드 정책 차이       | AA   | UI·SEC·BA   | SEC  |   P0 | 파일 마스킹 기준   |
| SEC-06 | DB 컬럼 암호화         | 암호화 대상·검색·인덱스·성능    | DA   | SEC·DBA·AA  | SEC  |   P0 | DB 암호화 방안     |
| SEC-07 | 전문 필드 암호화       | 대상 필드·알고리즘·길이·버전    | SEC  | MCA·EAI·AA  | SA   |   P0 | 필드암호화 방안    |
| SEC-08 | 암복호화 수행 위치     | UI·MCA·EAI·서버 Adapter 경계    | AA   | SEC·FW·MCA  | SEC  |   P0 | 암복호화 구조도    |
| SEC-09 | 암호키 관리            | 생성·저장·사용·교체·폐기        | SEC  | TA·OPS      | SA   |   P0 | 키관리 절차        |
| SEC-10 | 전송구간 암호화        | TLS 적용구간·인증서·암호군      | TA   | SEC·MCA·EAI | SEC  |   P0 | TLS 적용설계       |
| SEC-11 | 복호화 권한            | 원문조회 권한·목적·승인·감사    | SEC  | BA·AA·OPS   | SA   |   P0 | 복호화 권한표      |
| SEC-12 | 비운영 데이터          | 비식별·합성·반출 통제           | DA   | SEC·QA·DBA  | SEC  |   P0 | 테스트 데이터 기준 |
| SEC-13 | 개인정보 감사          | 조회·변경·다운로드·복호화 이력  | SEC  | AA·OPS·BA   | SA   |   P1 | 감사 이벤트 목록   |
| SEC-14 | 마스킹 예외            | 예외 사용자·기간·사유·만료      | SEC  | BA·감사     | SA   |   P0 | 예외 승인서        |
| SEC-15 | Secret 관리            | 비밀번호·Key·Token의 외부관리   | SEC  | DVO·TA·FW   | SEC  |   P0 | Secret 관리기준    |

---

# 7. 인증·인가·JWT·세션

| ID      | 아키텍처 의사결정 사항 | 주요 결정내용                   | 주관 | 협의       | 승인 | 우선 | 주요 산출물       |
| ------- | ---------------------- | ------------------------------- | ---- | ---------- | ---- | ---: | ----------------- |
| AUTH-01 | SSO 인증 흐름          | 인증 원천·사용자 확인·결과 전달 | SEC  | UI·MCA·AA  | SA   |   P0 | SSO 흐름도        |
| AUTH-02 | 세션·JWT 전략          | 시스템별 세션·토큰 적용 범위    | AA   | SEC·TA     | SA   |   P0 | 인증전략서        |
| AUTH-03 | JWT 발급 주체          | 발급 서버·Private Key 보유 위치 | SEC  | AA·FW      | SA   |   P0 | JWT 발급설계      |
| AUTH-04 | JWT 검증 위치          | Gateway·업무 WAR Filter 적용    | AA   | SEC·FW     | SEC  |   P0 | JWT 검증설계      |
| AUTH-05 | JWT Claim              | 사용자·조직·채널·권한 최소정보  | SEC  | BA·AA      | SA   |   P0 | Claim 정의서      |
| AUTH-06 | Claim–Header 정합성    | 보정·비교·불일치 거부 기준      | FW   | SEC·MCA·AA | SEC  |   P0 | Header 검증기준   |
| AUTH-07 | Access Token 수명      | 만료시간·Clock Skew·재로그인    | SEC  | UI·AA      | SEC  |   P0 | Token 수명정책    |
| AUTH-08 | Refresh Token          | 저장·Rotation·재사용 차단       | SEC  | AA·OM      | SEC  |   P0 | Refresh 정책      |
| AUTH-09 | Token 폐기             | 로그아웃·강제폐기·DenyList      | SEC  | OM·AA·OPS  | SA   |   P1 | Token 폐기설계    |
| AUTH-10 | 기능권한               | 메뉴·버튼·ServiceId 권한 연결   | AA   | SEC·BA·UI  | SEC  |   P0 | 기능권한 Matrix   |
| AUTH-11 | 데이터권한             | 조직·지점·고객 범위 검증        | BA   | SEC·AA·DA  | SEC  |   P0 | 데이터권한 Matrix |
| AUTH-12 | 관리자 권한            | 관리자 분리·승인·감사           | SEC  | OM·OPS     | SA   |   P0 | 관리자 통제기준   |
| AUTH-13 | 인증 예외 URL          | 로그인·JWKS·Health 예외목록     | SEC  | AA·FW      | SEC  |   P0 | 예외 URL 목록     |
| AUTH-14 | 동시 로그인            | 허용대수·장치·강제종료 기준     | SEC  | BA·OM      | SA   |   P1 | 동시로그인 정책   |

Gateway가 없는 경우에도 JWT 검증을 업무 Handler에 넣지 않고 업무 WAR의 공통 인증 Filter에서 처리한 후 TCF/STF가 인증 문맥과 Header 정합성을 검증하는 구조를 적용해야 합니다.

---

# 8. MCA·채널·전문 아키텍처

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                    | 주관 | 협의      | 승인 | 우선 | 주요 산출물      |
| ------ | ---------------------- | -------------------------------- | ---- | --------- | ---- | ---: | ---------------- |
| MCA-01 | 채널 표준 전문         | Header·Body·Result 구조          | MCA  | FW·UI·AA  | SA   |   P0 | 표준 전문 정의서 |
| MCA-02 | 전문 Header            | ServiceId·사용자·채널·지점·Trace | MCA  | FW·SEC    | AA   |   P0 | Header 필드표    |
| MCA-03 | 전문 필드 속성         | 형식·길이·필수·코드·민감도       | MCA  | DA·AA·BA  | AA   |   P0 | 필드 정의서      |
| MCA-04 | 채널 식별자            | channelId·terminalId·deviceId    | MCA  | SEC·UI    | AA   |   P0 | 채널 식별기준    |
| MCA-05 | 전문 Validation        | UI·MCA·서버 검증 책임            | MCA  | UI·FW·AA  | AA   |   P0 | 검증 책임표      |
| MCA-06 | 전문 데이터 변환       | UI 모델과 서버 DTO 변환 위치     | MCA  | UI·AA     | AA   |   P1 | 변환 설계서      |
| MCA-07 | 채널 라우팅            | 업무코드·Context·ServiceId 기준  | MCA  | TA·AA     | SA   |   P0 | 라우팅 규칙      |
| MCA-08 | 채널 Timeout           | UI·MCA·서버 Timeout 관계         | MCA  | TA·AA·FW  | TA   |   P0 | Timeout Matrix   |
| MCA-09 | 전문 버전관리          | 버전필드·하위호환·폐기기간       | MCA  | UI·AA·EAI | SA   |   P1 | 전문 버전정책    |
| MCA-10 | 채널 오류 변환         | 서버 오류와 단말 오류 매핑       | MCA  | FW·UI·BA  | AA   |   P0 | 오류 매핑표      |
| MCA-11 | 전문 암호화            | 암호화 필드·Key ID·버전          | SEC  | MCA·AA    | SA   |   P0 | 암호화 전문규격  |
| MCA-12 | 전문 길이 제한         | 최대크기·압축·대용량 분리        | MCA  | TA·AA     | TA   |   P1 | 전문 크기기준    |

---

# 9. EAI·외부연계·업무 간 연계

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                  | 주관 | 협의        | 승인 | 우선 | 주요 산출물         |
| ------ | ---------------------- | ------------------------------ | ---- | ----------- | ---- | ---: | ------------------- |
| INT-01 | EAI 적용범위           | 외부·기간계·비동기·변환 대상   | EAI  | AA·BA       | SA   |   P0 | 연계 분류표         |
| INT-02 | 동기·비동기 선택       | 즉시성·처리량·장애격리 기준    | EAI  | AA·TA·BA    | SA   |   P0 | 연계방식 결정표     |
| INT-03 | 연계 표준 전문         | 요청·응답·오류·추적정보        | EAI  | AA·DA       | SA   |   P0 | 연계 전문규격       |
| INT-04 | 업무 WAR 간 연계       | ServiceId 기반 표준 호출       | AA   | FW·EAI      | SA   |   P0 | WAR 연계기준        |
| INT-05 | 연계 Timeout           | 연결·응답·전체 Timeout 구분    | EAI  | TA·AA·FW    | TA   |   P0 | 연계 Timeout표      |
| INT-06 | 재시도 정책            | 대상오류·횟수·간격·Backoff     | EAI  | AA·BA·OPS   | AA   |   P0 | Retry 정책          |
| INT-07 | 멱등성                 | 중복키·처리상태·응답 재사용    | AA   | EAI·DA·FW   | SA   |   P0 | 멱등성 설계         |
| INT-08 | 부분 성공              | 로컬 DB와 원격 처리 상태관리   | AA   | EAI·DA·BA   | SA   |   P0 | 상태전이 설계       |
| INT-09 | 보상 처리              | 보상거래·수동복구·재처리       | AA   | EAI·BA·OPS  | SA   |   P0 | 보상처리 방안       |
| INT-10 | 전문 대사              | 송수신 건수·금액·상태 대사     | EAI  | DA·OPS      | SA   |   P1 | 대사 설계서         |
| INT-11 | 연계 오류 매핑         | 외부코드와 내부 오류코드 관계  | EAI  | FW·AA·BA    | AA   |   P0 | 오류 매핑표         |
| INT-12 | Circuit Breaker        | 차단조건·Half-open·복구        | EAI  | TA·AA·OPS   | TA   |   P1 | 장애격리 기준       |
| INT-13 | 연계 Pool 분리         | 외부시스템별 Thread·Connection | EAI  | TA·AA       | TA   |   P1 | 연계 자원설계       |
| INT-14 | 연계 변경관리          | 버전·병행운영·폐기·통보        | EAI  | 외부기관·AA | SA   |   P1 | 인터페이스 변경절차 |
| INT-15 | 파일 연계              | 파일명·암호화·대사·재처리      | EAI  | DA·SEC·OPS  | SA   |   P1 | 파일연계 기준       |
| INT-16 | 메시지 연계            | Topic·Queue·순서·재처리·DLQ    | EAI  | AA·TA·OPS   | SA   |   P1 | 메시징 설계서       |

---

# 10. 데이터·DB·SQL 아키텍처

| ID      | 아키텍처 의사결정 사항 | 주요 결정내용                | 주관 | 협의        | 승인 | 우선 | 주요 산출물        |
| ------- | ---------------------- | ---------------------------- | ---- | ----------- | ---- | ---: | ------------------ |
| DATA-01 | 데이터 소유권          | 테이블·변경권한·책임 도메인  | DA   | AA·BA       | SA   |   P0 | 데이터 소유권표    |
| DATA-02 | 타 업무 DB 접근        | 직접 조회·변경 허용범위      | DA   | AA·DBA      | SA   |   P0 | DB 접근원칙        |
| DATA-03 | 논리 데이터 모델       | 엔터티·관계·식별자·용어      | DA   | BA·AA       | SA   |   P0 | 논리모델           |
| DATA-04 | 물리 데이터 모델       | 테이블·컬럼·타입·제약조건    | DA   | DBA·AA      | DA   |   P0 | 물리모델           |
| DATA-05 | 표준용어·도메인        | 용어·영문명·타입·길이        | DA   | BA·DBA      | DA   |   P0 | 표준용어사전       |
| DATA-06 | 공통 컬럼              | 생성·수정·사용자·지점·버전   | DA   | AA·SEC      | DA   |   P0 | 공통컬럼 기준      |
| DATA-07 | 논리삭제               | 삭제여부·삭제일·복구·조회    | DA   | BA·SEC      | SA   |   P0 | 삭제정책           |
| DATA-08 | 데이터 보존            | 보존기간·Archive·폐기        | DA   | SEC·BA·OPS  | SA   |   P1 | 보존·폐기정책      |
| DATA-09 | 조회 건수 제한         | 화면·API·엑셀 최대건수       | DA   | AA·UI·DBA   | AA   |   P0 | 조회제한 기준      |
| DATA-10 | 페이징 방식            | Offset·Keyset·총건수 조회    | DA   | DBA·UI·AA   | DA   |   P0 | 페이징 기준        |
| DATA-11 | SQL 작성표준           | Bind·컬럼명시·Join·Subquery  | DBA  | DA·AA       | DA   |   P0 | SQL 표준           |
| DATA-12 | 인덱스 설계            | 조회조건·정렬·분포·DML 비용  | DBA  | DA·AA·TA    | DA   |   P1 | 인덱스 설계서      |
| DATA-13 | Lock 처리              | Lock Timeout·Deadlock·재시도 | DBA  | DA·AA·OPS   | DA   |   P1 | Lock 대응기준      |
| DATA-14 | 동시성 제어            | 낙관적 Lock·비관적 Lock 적용 | DA   | DBA·AA·BA   | SA   |   P1 | 동시성 설계        |
| DATA-15 | 대량 데이터            | 온라인·배치·파일 처리경계    | DA   | TA·AA·DBA   | SA   |   P1 | 대량처리 기준      |
| DATA-16 | DB 변경배포            | DDL·DML·선후관계·Rollback    | DBA  | DVO·DA·AA   | DA   |   P1 | DB 배포절차        |
| DATA-17 | 테스트 데이터          | 비식별·합성·초기화·재현      | DA   | SEC·QA      | SEC  |   P0 | 테스트 데이터 기준 |
| DATA-18 | DB 감사                | 중요 테이블 변경·조회 추적   | DA   | SEC·DBA·OPS | SEC  |   P1 | DB 감사기준        |

DB 객체명은 단순 명칭이 아니라 `ServiceId → Handler → DAO·Mapper → SQL ID → Table·Column → 거래로그·감사로그` 추적관계와 일치해야 합니다.

---

# 11. 오류·Timeout·트랜잭션·안정성

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                    | 주관 | 협의       | 승인 | 우선 | 주요 산출물      |
| ------ | ---------------------- | -------------------------------- | ---- | ---------- | ---- | ---: | ---------------- |
| REL-01 | 오류 분류              | 입력·인증·권한·업무·시스템·연계  | FW   | AA·BA·OPS  | AA   |   P0 | 오류 분류표      |
| REL-02 | 오류코드 체계          | 공통·업무·외부·DB 오류영역       | FW   | AA·BA      | AA   |   P0 | 오류코드 규칙    |
| REL-03 | 사용자 메시지          | 사용자 안내와 내부정보 분리      | BA   | UI·FW·AA   | AA   |   P0 | 메시지 관리기준  |
| REL-04 | 예외 변환              | Java·DB·외부 예외 변환 위치      | FW   | AA·DA·EAI  | AA   |   P0 | 예외처리 기준    |
| REL-05 | Timeout 계층           | UI>MCA>WAS>DB·연계 관계          | TA   | AA·MCA·EAI | SA   |   P0 | Timeout Matrix   |
| REL-06 | ServiceId Timeout      | 거래별 기본값·예외·OM 관리       | AA   | FW·OPS·BA  | TA   |   P0 | Timeout 정책표   |
| REL-07 | DB Timeout             | Query·Lock·Connection Timeout    | DBA  | DA·AA·TA   | DA   |   P0 | DB Timeout 기준  |
| REL-08 | Timeout 이후 상태      | 응답 종료와 실제처리 상태 구분   | AA   | DA·EAI·OPS | SA   |   P0 | Timeout 상태설계 |
| REL-09 | 재시도 적격성          | 조회·멱등거래·일시오류 구분      | AA   | EAI·FW·BA  | SA   |   P0 | 재시도 기준      |
| REL-10 | 멱등성 Key             | 생성주체·저장기간·중복응답       | FW   | UI·MCA·DA  | AA   |   P0 | 멱등성 표준      |
| REL-11 | Rollback 기준          | 업무예외·시스템예외·Checked 예외 | AA   | FW·DA      | SA   |   P0 | Rollback Matrix  |
| REL-12 | 부분 실패              | Commit 상태·보상·상태조회        | AA   | DA·EAI·BA  | SA   |   P0 | 부분실패 처리안  |
| REL-13 | Circuit Breaker        | 오류율·지연시간·복구조건         | EAI  | TA·OPS·AA  | TA   |   P1 | Circuit 기준     |
| REL-14 | Bulkhead               | 업무·외부연계 자원격리           | TA   | AA·EAI·OPS | SA   |   P1 | 자원격리 설계    |
| REL-15 | 사용자 재처리          | 재조회·재요청·상태확인 UX        | BA   | UI·AA·OPS  | AA   |   P1 | 사용자 대응기준  |

---

# 12. 기술·인프라·용량 아키텍처

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                  | 주관 | 협의       | 승인  | 우선 | 주요 산출물      |
| ------ | ---------------------- | ------------------------------ | ---- | ---------- | ----- | ---: | ---------------- |
| INF-01 | 물리 배포구조          | 센터·VM·Tomcat·WAR 구성        | TA   | SA·AA·OPS  | SA    |   P0 | 물리 구성도      |
| INF-02 | Tomcat 업무그룹        | A/B 그룹·업무 배치기준         | TA   | AA·OPS     | SA    |   P0 | 업무 배치표      |
| INF-03 | 단일 Tomcat 다중 WAR   | 공유자원·장애범위·허용 WAR 수  | TA   | AA·OPS     | SA    |   P0 | 다중 WAR 방안    |
| INF-04 | JVM Heap               | Heap·Metaspace·Direct Memory   | TA   | AA·OPS     | TA    |   P1 | JVM 설정기준     |
| INF-05 | GC 정책                | Collector·Pause 목표·로그      | TA   | OPS·AA     | TA    |   P1 | GC 적용기준      |
| INF-06 | Tomcat Thread          | maxThreads·Queue·Busy 임계치   | TA   | AA·FW      | TA    |   P1 | Thread 산정서    |
| INF-07 | HikariCP Pool          | WAR별 Pool·전체 DB Session     | TA   | DA·DBA·AA  | TA·DA |   P1 | DB Pool 산정서   |
| INF-08 | Port·Context           | 업무코드·WAR·URL 정합성        | TA   | AA·DVO·MCA | AA    |   P0 | Port·Context표   |
| INF-09 | 공통 Library           | Tomcat lib와 WAR 내장 기준     | AA   | TA·FW      | SA    |   P0 | Library 배치기준 |
| INF-10 | 환경설정 분리          | Profile·환경변수·외부설정      | DVO  | TA·SEC·AA  | TA    |   P0 | 환경설정 기준    |
| INF-11 | Health Check           | Liveness·Readiness·Dependency  | TA   | OPS·AA·FW  | TA    |   P1 | Health 설계서    |
| INF-12 | L4·Apache 라우팅       | Context·업무그룹·Drain 기준    | TA   | MCA·AA·OPS | SA    |   P0 | 라우팅 설계서    |
| INF-13 | 무중단 배포            | 순차배포·Drain·검증·복귀       | DVO  | TA·OPS·AA  | TA    |   P1 | 배포절차         |
| INF-14 | Rollback               | WAR·설정·DB 복구 선후관계      | DVO  | TA·DBA·AA  | SA    |   P0 | Rollback 절차    |
| INF-15 | 장애 격리              | VM·JVM·Tomcat·WAR Blast Radius | TA   | AA·OPS     | SA    |   P1 | 장애격리 설계    |
| INF-16 | 용량 Baseline          | 사용자·TPS·p95·Stress·DR       | TA   | AA·DA·SA   | SA    |   P1 | 용량 기준선      |
| INF-17 | 가용성                 | Active-Active·Failover·점검    | TA   | SA·OPS     | SA    |   P1 | 가용성 설계      |
| INF-18 | DR                     | RTO·RPO·전환·복귀·데이터복구   | TA   | DA·OPS·SA  | SA    |   P2 | DR 설계서        |
| INF-19 | 시간 동기화            | NTP·Timezone·로그시간 기준     | TA   | OPS·AA     | TA    |   P1 | 시간관리 기준    |
| INF-20 | 인증서 관리            | TLS 인증서 발급·교체·만료      | TA   | SEC·OPS    | SEC   |   P1 | 인증서 관리절차  |

하나의 Tomcat에 여러 WAR를 배포하면 업무별 Spring Context는 분리되지만 JVM·Heap·GC·Connector Thread와 장애범위는 공유된다는 전제에서 배치 구조를 결정해야 합니다.

---

# 13. 배치·파일·캐시 아키텍처

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                   | 주관 | 협의      | 승인 | 우선 | 주요 산출물     |
| ------ | ---------------------- | ------------------------------- | ---- | --------- | ---- | ---: | --------------- |
| BFC-01 | 온라인·배치 경계       | 즉시처리와 대량 후속처리 구분   | AA   | BA·DA·TA  | SA   |   P1 | 처리유형 기준   |
| BFC-02 | Batch Job 구조         | Job·Step·Tasklet·Reader·Writer  | AA   | FW·DA     | SA   |   P1 | Batch 설계기준  |
| BFC-03 | Scheduler              | 중앙·분산 스케줄·중복실행 통제  | TA   | AA·OPS    | SA   |   P1 | Scheduler 설계  |
| BFC-04 | Batch 재시작           | Checkpoint·재시작 위치·상태관리 | AA   | DA·OPS    | SA   |   P1 | 재시작 기준     |
| BFC-05 | Batch 오류처리         | Skip·Retry·중단·수동재처리      | AA   | BA·OPS    | SA   |   P1 | Batch 오류기준  |
| BFC-06 | 파일명 표준            | 송수신 시스템·일자·순번·버전    | EAI  | DA·OPS    | SA   |   P1 | 파일명 규칙     |
| BFC-07 | 대용량 파일            | 분할·Streaming·임시공간·정리    | TA   | AA·DA     | TA   |   P1 | 대용량 파일기준 |
| BFC-08 | 파일 암호화            | 암호화·서명·압축·Key 관리       | SEC  | EAI·TA    | SEC  |   P1 | 파일보안 기준   |
| BFC-09 | 파일 대사              | 건수·Hash·금액·상태 확인        | EAI  | DA·OPS    | SA   |   P1 | 파일대사 기준   |
| BFC-10 | Cache 대상             | 공통코드·기준정보·조회데이터    | AA   | DA·BA·OPS | SA   |   P1 | Cache 대상목록  |
| BFC-11 | Cache Key              | 업무·도메인·버전·사용자 범위    | AA   | DA·FW     | AA   |   P1 | Cache Key 기준  |
| BFC-12 | Cache TTL              | 데이터 변경주기·정합성·장애     | AA   | DA·BA     | SA   |   P1 | TTL 정책표      |
| BFC-13 | Cache 무효화           | 변경 즉시·이벤트·시간기반       | AA   | DA·EAI    | SA   |   P1 | 무효화 설계     |
| BFC-14 | Cache 장애             | Cache 우회·Fallback·DB 부하     | AA   | TA·OPS·DA | SA   |   P2 | Cache 장애방안  |

---

# 14. 운영·로그·감사·모니터링

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                     | 주관 | 협의        | 승인 | 우선 | 주요 산출물     |
| ------ | ---------------------- | --------------------------------- | ---- | ----------- | ---- | ---: | --------------- |
| OPS-01 | 애플리케이션 로그      | 구조화 형식·Level·공통필드        | FW   | OPS·SEC·AA  | AA   |   P0 | 로그 표준       |
| OPS-02 | 거래로그               | 시작·종료·상태·시간·오류          | FW   | OM·OPS·AA   | AA   |   P0 | 거래로그 설계   |
| OPS-03 | 감사로그               | 중요 조회·변경·다운로드·권한      | SEC  | AA·BA·OPS   | SA   |   P0 | 감사로그 설계   |
| OPS-04 | SQL 로그               | Mapper ID·시간·건수·파라미터 통제 | DBA  | DA·OPS·AA   | DA   |   P1 | SQL 로그기준    |
| OPS-05 | 연계로그               | 요청·응답·Timeout·Retry·대사      | EAI  | OPS·AA      | SA   |   P0 | 연계로그 기준   |
| OPS-06 | 로그 상관관계          | GUID·TraceId·ServiceId·UserId     | FW   | MCA·EAI·OPS | AA   |   P0 | 로그 필드표     |
| OPS-07 | 로그 보존              | 종류별 보존·압축·백업·폐기        | OPS  | SEC·TA·DA   | SEC  |   P1 | 로그 보존정책   |
| OPS-08 | OM Service Catalog     | ServiceId·Handler·Timeout·상태    | FW   | AA·OPS·DEV  | AA   |   P0 | Catalog 설계    |
| OPS-09 | 거래통제               | 거래별 허용·차단·시간대·사유      | OM   | FW·AA·OPS   | SA   |   P0 | 거래통제 기준   |
| OPS-10 | Slow ServiceId         | 업무유형별 임계치·등급            | OPS  | AA·TA·FW    | SA   |   P1 | Slow 기준표     |
| OPS-11 | Slow SQL               | SQL ID·실행시간·건수 기준         | DBA  | DA·OPS      | DA   |   P1 | Slow SQL 기준   |
| OPS-12 | 런타임 진단            | JVM·Thread·Pool·ServiceId·SQL     | OPS  | TA·AA·DA    | SA   |   P1 | 런타임 진단설계 |
| OPS-13 | 알림 정책              | 경고·심각·통보·에스컬레이션       | OPS  | 전 영역     | SA   |   P2 | 알림 Matrix     |
| OPS-14 | 장애 원인코드          | Thread·GC·Pool·SQL·연계 분류      | OPS  | TA·AA·DA    | SA   |   P2 | 원인코드표      |
| OPS-15 | Runbook                | 장애유형별 확인·조치·복구         | OPS  | 전 영역     | SA   |   P2 | 운영 Runbook    |
| OPS-16 | 운영 변경관리          | 설정·정책·기준정보 변경이력       | OPS  | DVO·AA·TA   | SA   |   P1 | 운영변경 절차   |
| OPS-17 | 운영자 권한            | 조회·조치·정책변경 권한 분리      | OPS  | SEC·OM      | SEC  |   P1 | 운영권한 Matrix |
| OPS-18 | 장애 보고              | 발생·영향·원인·조치·재발방지      | OPS  | PMO·전 영역 | SA   |   P2 | 장애보고서      |

---

# 15. DevOps·CI/CD·품질검증

| ID     | 아키텍처 의사결정 사항 | 주요 결정내용                   | 주관 | 협의       | 승인 | 우선 | 주요 산출물     |
| ------ | ---------------------- | ------------------------------- | ---- | ---------- | ---- | ---: | --------------- |
| QLT-01 | Git Branch 전략        | main·develop·feature·release    | DVO  | AA·QA      | SA   |   P0 | Branch 정책     |
| QLT-02 | Commit 기준            | 논리 변경단위·메시지·ServiceId  | DVO  | DEV·QA     | AA   |   P0 | Commit 규칙     |
| QLT-03 | Pull Request           | 리뷰어·필수항목·승인수          | DVO  | AA·QA      | SA   |   P0 | PR 템플릿       |
| QLT-04 | 빌드 환경              | JDK·Gradle Wrapper·Repository   | DVO  | FW·TA      | AA   |   P0 | 빌드 기준       |
| QLT-05 | Artifact 버전          | WAR·JAR·Commit·배포버전 관계    | DVO  | AA·OPS     | AA   |   P1 | 버전정책        |
| QLT-06 | 계층 자동검증          | 계층 우회·직접 Mapper 호출 차단 | FW   | AA·QA      | AA   |   P1 | ArchUnit 규칙   |
| QLT-07 | ServiceId 중복검사     | 소스·Handler·OM 중복·누락       | FW   | AA·OM·QA   | AA   |   P0 | ServiceId 검사  |
| QLT-08 | 명명규칙 검사          | 패키지·클래스·설정·DB 명명      | DVO  | AA·DA·FW   | AA   |   P1 | 정적검사 규칙   |
| QLT-09 | 보안 정적분석          | SAST·SCA·Secret Scan            | SEC  | DVO·AA     | SEC  |   P1 | 보안 Gate       |
| QLT-10 | 단위테스트             | 정상·경계·오류·Rule 검증        | QA   | DEV·AA     | AA   |   P0 | 단위테스트 기준 |
| QLT-11 | 통합테스트             | TCF·DB·연계·트랜잭션 검증       | QA   | AA·DA·EAI  | SA   |   P1 | 통합테스트 기준 |
| QLT-12 | 계약테스트             | MCA·API·EAI 전문 호환성         | QA   | MCA·EAI·AA | SA   |   P1 | Contract Test   |
| QLT-13 | 성능시험               | 평시·피크·Stress·장시간·DR      | QA   | TA·AA·DA   | SA   |   P1 | 성능시험 계획   |
| QLT-14 | 보안시험               | 인증·권한·마스킹·취약점         | SEC  | QA·AA·UI   | SEC  |   P1 | 보안시험 계획   |
| QLT-15 | 장애시험               | DB·WAS·연계·Network·Cache       | QA   | TA·AA·OPS  | SA   |   P2 | 장애시험 계획   |
| QLT-16 | 배포 승인 Gate         | 결함등급·증적·승인조건          | QA   | DVO·OPS·AA | SA   |   P1 | 배포 Gate       |
| QLT-17 | 배포 후 검증           | Health·대표거래·로그·DB 확인    | OPS  | DVO·QA·AA  | TA   |   P1 | 배포검증표      |
| QLT-18 | Drift 검증             | 설계·소스·OM·설정 불일치 탐지   | QA   | AA·FW·DVO  | SA   |   P2 | Drift 검사      |

---

# 16. 우선 확정 대상

## 16.1 P0 핵심 의사결정

| 순서 | 핵심 결정사항                   | 주관 영역  |
| ---: | ------------------------------- | ---------- |
|    1 | 아키텍처 의사결정·ADR·예외 절차 | SA         |
|    2 | 업무코드·도메인·WAR 경계        | SA·AA      |
|    3 | 화면 ID·ServiceId·거래코드      | UI·FW·AA   |
|    4 | Java·DB·전문 명명규칙           | AA·DA·MCA  |
|    5 | 표준 요청·응답 전문             | MCA·FW     |
|    6 | TCF 공통 실행 흐름              | FW·AA      |
|    7 | 애플리케이션 계층 책임          | AA         |
|    8 | DTO·Validation 기준             | AA·UI      |
|    9 | 트랜잭션·Rollback               | AA         |
|   10 | 오류코드·예외·메시지            | FW·AA      |
|   11 | Timeout 계층                    | TA·AA      |
|   12 | 재시도·멱등성·부분성공          | AA·EAI     |
|   13 | 개인정보 분류·마스킹            | DA·SEC·AA  |
|   14 | 전문 필드 암호화·키관리         | SEC        |
|   15 | SSO·JWT·권한 구조               | SEC·AA     |
|   16 | MCA·EAI·업무 WAR 연계 경계      | SA·AA·EAI  |
|   17 | 데이터 소유권·타 업무 DB 접근   | DA         |
|   18 | 로그·감사·추적정보              | FW·SEC·OPS |
|   19 | Tomcat·WAR·Context 배치         | TA         |
|   20 | CI/CD 필수 품질 Gate            | DVO·QA     |

---

# 17. 권장 방안서 작성 순서

| 순서 | 방안서                                | 주요 참여 영역 |
| ---: | ------------------------------------- | -------------- |
|    1 | 개인정보 마스킹 및 원문조회 통제 방안 | DA·SEC·AA·UI   |
|    2 | 전문 필드 암호화 및 키관리 방안       | SEC·MCA·EAI·AA |
|    3 | 통합 명명·식별·추적성 방안            | SA·AA·UI·DA·FW |
|    4 | 표준 전문 및 ServiceId 운영 방안      | MCA·FW·AA·OM   |
|    5 | TCF 실행 흐름 및 계층 책임 방안       | FW·AA          |
|    6 | 인증·JWT·권한·Header 정합성 방안      | SEC·AA·FW      |
|    7 | 오류·예외·표준 응답 방안              | FW·AA·UI       |
|    8 | 트랜잭션·Timeout·멱등성 방안          | AA·TA·EAI·DA   |
|    9 | MCA·EAI·업무 WAR 연계 방안            | SA·AA·EAI      |
|   10 | 데이터 소유권 및 DB 접근 통제 방안    | DA·AA·DBA      |
|   11 | 로그·감사·운영 추적 방안              | FW·SEC·OPS     |
|   12 | Tomcat·WAR·JVM·DB Pool 배치 방안      | TA·AA·DA       |
|   13 | 파일·엑셀·배치·대용량 처리 방안       | AA·DA·TA       |
|   14 | CI/CD·Architecture Quality Gate 방안  | DVO·QA·AA      |
|   15 | 운영진단·장애대응·Runbook 방안        | OPS·TA·AA·DA   |

---

# 18. 관리 원칙

각 의사결정 사항은 다음 상태로 관리한다.

```text
미착수
→ 분석 중
→ 대안 검토
→ PoC 진행
→ 승인 대기
→ 승인
→ 표준 반영
→ 공통모듈 반영
→ 자동검증 반영
→ 적용 완료
→ 폐기·대체
```

최종 완료 기준은 다음과 같다.

```text
ADR 승인
+ 개발표준 반영
+ 공통 샘플 또는 모듈 제공
+ 테스트 기준 제공
+ CI/CD 자동검증 반영
+ 업무팀 적용 확인
= 아키텍처 의사결정 TASK 완료
```
