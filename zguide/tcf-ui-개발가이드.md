# tcf-ui 개발자 가이드



> **역할:** 온라인 거래 테스트 UI + **OM 운영관리 포털** Relay  

> **포트:** **8099** · Context **`/ui`** (ztomcat)



---



## 1. 이 모듈이 하는 일



WebTopSuite 없이 브라우저에서 TCF JSON 전문을 작성·전송·응답 확인.



```

브라우저 → tcf-ui (/api/relay, /api/gateway/om, /api/updownload)

         → 업무 WAS / tcf-om (직접) 또는 tcf-gateway (OM JWT 선택)

```



> **업무 거래 relay**는 Gateway **미경유** — 업무 WAS에 직접 연결합니다.  

> **OM Admin JWT 모드**는 `om-admin.js`가 `Authorization: Bearer`로 **tcf-gateway**(:8100)를 경유할 수 있습니다 (`omGatewayEnabled`).  

> 모든 온라인 거래를 Gateway로내려면 [tcf-uj-개발가이드.md](./tcf-uj-개발가이드.md)를 사용하세요.



---



## 2. 5분 빠른 시작



```bash

gradle :sv-service:bootRun   # 8086

gradle :tcf-om:bootRun       # 8097

gradle :tcf-ui:bootRun       # 8099



# 거래 테스트

http://localhost:8099/sv/index.html



# OM Admin (admin01 / nsight01!)

http://localhost:8099/om/admin/login.html

```



JWT Gateway OM Admin 테스트 시 추가:



```bash

gradle :tcf-jwt:bootRun      # 8110

gradle :tcf-gateway:bootRun  # 8100 (local JWT enabled)

# application-local.yml: om-gateway-enabled: true

```



---



## 3. 실행



| | |

|---|---|

| bootRun | `gradle :tcf-ui:bootRun` |

| 스크립트 | `tcf-scripts/run-local.bat ui` |

| ztomcat | `http://localhost:8080/ui/` |



**메인:** `com.nh.nsight.tcf.ui.NsightTcfUiApplication`



---



## 4. 주요 API



| API | 설명 |

|-----|------|

| `GET /api/business-modules` | 업무 목록 |

| `POST /api/relay/{code}/online` | 온라인 Relay (**직접** WAS, Cookie) |

| `GET /api/gateway/om/target-url` | OM Gateway relay 대상 URL |

| `POST /api/gateway/om/online` | OM Gateway Relay (**JWT Bearer**) |

| `POST /api/updownload/upload` | 파일 업로드 (tcf-om) |

| `GET /api/config` | 배포 모드·`omGatewayEnabled` |



---



## 5. 배포 모드



| 모드 | 설정 | OM Admin URL |

|------|------|--------------|

| bootrun | `deployment-mode: bootrun` | `:8099/om/admin/...` |

| tomcat | `deployment-mode: tomcat` | `:8080/ui/om/admin/...` |



공통 JS: `static/_shared/ui-context.js`, `om-admin.js` (`shouldUseOmGateway`, `callViaGateway`)



---



## 6. 화면



| URL | 설명 |

|-----|------|

| `/index.html` | 업무 허브 |

| `/{code}/index.html` | 단일 거래 |

| `/sv/sample-list.html` | SV 페이징 샘플 |

| `/om/admin/*` | OM 운영 포털 전체 |

| `/jwt/admin/*` | JWT Admin (토큰·정책·이력) |

| `/ud/updownload.html` | UD 파일 |



---



## 7. 샘플 JSON



`tcf-ui/src/main/resources/sample-requests/` — 업무별 inquiry JSON



```bash

tcf-scripts/curl-sample.bat sv

```



---



## 8. 패키지 구조



```

com.nh.nsight.tcf.ui

├── client/              TransactionRelayService, GatewayRelayService, UpdownloadRelayService

├── application/service/ BusinessModuleCatalog

├── entry/web/           TcfApiController, UpdownloadApiController

└── support/             BusinessModuleDefinitions

```



---



## 9. tcf-uj와 비교



| | tcf-ui | tcf-uj |

|---|--------|--------|

| 포트 | 8099 | 8102 |

| 업무 Relay | WAS **직접** | **Gateway 경유** (Cookie) |

| OM JWT Gateway | `callViaGateway` (**tcf-ui** `om-admin.js`) | API만 (`/api/gateway/om/online`), JS 분기 없음 |

| JWT Admin | ✅ `/jwt/admin/*` | ✅ `/jwt/admin/*` |



---



## 10. 참고



| | |

|---|---|

| [tcf-ui/README.md](../tcf-ui/README.md) | |

| [tcf-uj-개발가이드.md](./tcf-uj-개발가이드.md) | Gateway UI |

| [tcf-scripts-개발가이드.md](./tcf-scripts-개발가이드.md) | 로컬 스크립트 |


