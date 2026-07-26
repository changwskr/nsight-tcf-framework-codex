# znsight-guide vs znsight-man audit

Guide docx numbers: 82, cache files: 82
Unmapped docx numbers (not in DOCX_BY_CHAPTER/APPENDIX): 1, 2, 3, 4, 5, 81, 82

## Issues

### ch14 ← docx-19 (LOW_COVERAGE)
- md: 14-명명-규칙.md
- guide: NSIGHT TCF 개발 매뉴얼 - 통합 (19).docx
- line coverage: 97% (undefined/undefined)
- sections: docx 23 → md 21, missing 4
- missing sections: 14.6 Package 명명 규칙 | 14.7 ServiceId 명명 규칙 | 14.8 거래코드 명명 규칙 | 14.13 Mapper XML / SQL ID 명명 규칙
- sample missing lines: `cc-service`, `bc-service`, `cm-service`, `bp-service`, `bd-service`, `cs-service`, `ct-service`

### ch17 ← docx-22 (LOW_COVERAGE)
- md: 17-거래코드-설계.md
- guide: NSIGHT TCF 개발 매뉴얼 - 통합 (22).docx
- line coverage: 100% (undefined/undefined)
- sections: docx 21 → md 26, missing 4
- missing sections: 17.5 처리유형 코드 표준 | 17.6 거래코드 채번 원칙 | 17.10 거래코드와 거래통제 연계 | 17.11 거래코드와 거래로그 연계
- sample missing lines: `아래 내용을 NSIGHT TCF 개발자 가이드 3차 - 17장으로 이어 붙이면 됩니다.`

### ch21 ← docx-26 (LOW_COVERAGE)
- md: 21-Header-작성-기준.md
- guide: NSIGHT TCF 개발 매뉴얼 - 통합 (26).docx
- line coverage: 99% (undefined/undefined)
- sections: docx 29 → md 22, missing 9
- missing sections: 21.13 ProcessingType 작성 기준 | 21.14 IdempotencyKey 작성 기준 | 21.15 ClientIp 작성 기준 | 21.22 Header 작성 예시 | 21.22.1 SV 고객 요약 조회 | 21.22.2 OM 사용자 수정 | 21.22.3 UD 파일 다운로드 | 21.23 Header 작성 시 금지 사항 | 21.24 개발자 체크리스트
- sample missing lines: `아래 내용을 NSIGHT TCF 개발자 가이드 3차 - 21장으로 이어 붙이면 됩니다.`

### ch32 ← docx-37 (LOW_COVERAGE)
- md: 32-예외처리-기준.md
- guide: NSIGHT TCF 개발 매뉴얼 - 통합 (37).docx
- line coverage: 100% (undefined/undefined)
- sections: docx 30 → md 25, missing 6
- missing sections: 32.20 예외 발생 위치별 개발 기준 | 32.20.1 Rule | 32.20.2 Service | 32.20.3 DAO | 32.20.4 Integration Adapter | 32.21 Rollback 기준

### ch61 ← docx-60 (LOW_COVERAGE)
- md: 61-코드-리뷰-기준.md
- guide: NSIGHT TCF 개발 매뉴얼 - 통합 (60).docx
- line coverage: 100% (undefined/undefined)
- sections: docx 32 → md 37, missing 6
- missing sections: 61.16 DAO / Mapper 리뷰 기준 | 61.17 MyBatis SQL 리뷰 기준 | 61.28 코드 리뷰 판정 기준 | 61.29 대표 코드 리뷰 금지 패턴 | 61.30 코드 리뷰 완료 기준 | 61.31 마무리말

## Unmapped guide docx

- docx-1: NSIGHT TCF 개발 매뉴얼 - 통합 (1).docx (403263 bytes)
- docx-2: NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 (2).docx (230530 bytes)
- docx-3: NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 (3).docx (383269 bytes)
- docx-4: NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 (4).docx (374039 bytes)
- docx-5: NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 (5).docx (380451 bytes)
- docx-81: NSIGHT TCF 개발 매뉴얼 - 통합 (81).docx (403263 bytes)
- docx-82: NSIGHT TCF 개발 매뉴얼 - 통합 (82).docx (229746 bytes)

## Chapters without docx mapping (use zdoc/zman fallback)

40, 45, 50, 53, 57, 60, 63, 66, 69, 73, 75, 78