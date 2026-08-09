# 2026-08-01 농협상호금융 spring 및 gradle 환경

농협상호금융(폐쇄망) 기준 Spring / Gradle 환경을 참고용으로 모아 둔 폴더입니다.  
이후 `pdmp-service` 로컬·사내 설정의 출발점이 되었습니다.

## 구성

| 경로 | 설명 |
|------|------|
| `pdmp/` | build.gradle, application.yml, log4j2, mpcoa9999 mapper 샘플 등 |
| `docs/` | 환경 관련 PDF 등 |

현재 저장소의 실행 모듈은 루트의 `pdmp-service/`를 사용합니다. 이 폴더는 **스냅샷·참고**로 두고, 소스 of truth는 `pdmp-service`입니다.
