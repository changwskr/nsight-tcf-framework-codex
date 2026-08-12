# Mapping coverage v0.2

PDMG 샘플 프로그램 시드 현황:

| programId | title | services | table |
|-----------|-------|----------|-------|
| mgcoa8888 | 이미지로그 | S0,D0 | TB_FW_IMAGE_LOG |
| mgcoa9000 | 거래 파라미터 | S0,C0,U0,D0 | TB_MG_TX_PARAM |
| mgcoa9001 | 거래통제 | S0,C0,U0,D0 | TB_MG_TX_CONTROL |
| mgcoa5530 | 마케팅희망고객 | S0 | TB_MK_CO_A_5530 |
| mgcoa9999 | 영업팁 실적 | S0 | TB_CR_AH_SALES_TIP_RACT |

검증:

```bat
gradlew.bat validatePdmg
```

기대: `PASS` 또는 UI/부가 경로에 대한 warning만.
