# PDMP Project Map

The only default target is `pdmp-service`.

| Concern | Current location |
| --- | --- |
| Program controller | `pdmp-service/src/main/java/nhnis/mp/co/a/controller/mpcoa8888Controller.java` |
| Service and DAO | `pdmp-service/src/main/java/nhnis/mp/co/a/service/mpcoa8888Service.java`; `.../dao/mpcoa8888Dao.java` |
| DTOs | `pdmp-service/src/main/java/nhnis/mp/co/a/dto/` |
| MyBatis mapper | `pdmp-service/src/main/resources/rdw.mp.co.a/mpcoa8888-ORA.xml` |
| TCF annotations and DTOs | `pdmp-service/src/main/java/nhnis/fw/tcf/` |
| Security and CORS | `pdmp-service/src/main/java/nhnis/mp/config/SecurityConfig.java`; `CorsProperties.java`; `WebMvcConfig.java` |
| H2 resources | `pdmp-service/src/main/resources/db/h2/schema.sql`; `data.sql` |
| Program tests | `pdmp-service/src/test/java/nhnis/mp/co/a/controller/`, `service/`, and `dao/` |
| Security tests | `pdmp-service/src/test/java/nhnis/mp/config/SecurityConfigTest.java` |

For `mpcoa8888`, preserve `Controller -> Service -> DAO -> MyBatis`, the `nhnis.mp.co.a` package family, and `/api/mp/co/a/8888` endpoints. The mapper table is `TB_CR_AH_SALES_TIP_RACT`; its key is `TRT_BRC`, `TRTMN_ENO`, `SALZ_TIP_KDC`, and `BAS_DT`.

Run focused tests first, then from `pdmp-service` use `gradlew.bat test` and `gradlew.bat war`. H2 resources validate local behavior only; Oracle behavior remains unverified without an approved Oracle environment.
