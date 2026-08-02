# PDMP Project Map

The resolved target root is `../pdmp-service`.

| Concern | Actual path |
| --- | --- |
| mpcoa8888 Controller | `src/main/java/nhnis/mp/co/a/controller/mpcoa8888Controller.java` |
| mpcoa8888 Service | `src/main/java/nhnis/mp/co/a/service/mpcoa8888Service.java` |
| mpcoa8888 DAO | `src/main/java/nhnis/mp/co/a/dao/mpcoa8888Dao.java` |
| mpcoa8888 MyBatis mapper | `src/main/resources/rdw.mp.co.a/mpcoa8888-ORA.xml` |
| Controller test | `src/test/java/nhnis/mp/co/a/controller/mpcoa8888ControllerTest.java` |
| Service test | `src/test/java/nhnis/mp/co/a/service/mpcoa8888ServiceTest.java` |
| DAO integration test | `src/test/java/nhnis/mp/co/a/dao/mpcoa8888DaoIntegrationTest.java` |
| Security test | `src/test/java/nhnis/mp/config/SecurityConfigTest.java` |
| Security configuration | `src/main/java/nhnis/mp/config/SecurityConfig.java` |
| JWT filter | `src/main/java/nhnis/fw/tcf/web/JwtAuthenticationFilter.java` |
| Transaction annotation | `src/main/java/nhnis/fw/tcf/TcfTransaction.java` |
| TCF header workflow | `src/main/java/nhnis/fw/tcf/stf/STF.java` |
| TCF aspect and MDC context | `src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java`; `src/main/java/nhnis/fw/tcf/context/TcfContextHolder.java`; `src/main/java/nhnis/fw/tcf/TcfMdcKeys.java` |
| TCF request/response types | `src/main/java/nhnis/fw/tcf/dto/StandardRequestDto.java`; `src/main/java/nhnis/fw/tcf/dto/StandardResponseDto.java`; `src/main/java/nhnis/fw/tcf/dto/StandardHeaderDto.java` |
| Local H2 setup | `src/main/resources/db/h2/schema.sql`; `src/main/resources/db/h2/data.sql` |

For `mpcoa8888`, retain the `nhnis.mp.co.a` package family and `/api/mp/co/a/8888` protected route. Its mapper uses `TB_CR_AH_SALES_TIP_RACT` with composite key `TRT_BRC`, `TRTMN_ENO`, `SALZ_TIP_KDC`, and `BAS_DT`.
