# PDMK UI (`pdmk-ui`)

`pdmk-service` 전문 테스트용 로컬 UI입니다. 네이밍은 `pdmk-service/docs/MK-NAMING_CONVENTION.md` 를 따른다.

아키텍처 **정본**: [`pdmk-service/docs/PDMK_아키텍처_정의서.md`](../pdmk-service/docs/PDMK_아키텍처_정의서.md) (v2.0).

## 실행

```powershell
# 1) pdmk-service (8080)
cd ..\pdmk-service
.\RUN.bat

# 2) pdmk-ui (8090) — pdmk-service를 HTTP로 중계
cd ..\pdmk-ui
.\RUN.bat
```

브라우저: http://localhost:8090

## 전문 형식

요청/응답 모두 `hdr_nhnis` + `dto` 구조입니다.

```json
{
  "hdr_nhnis": {
    "sys_comm": {
      "std_gbl_id": "c3d65cb1a54a43838688b76afe82521e",
      "rms_svc_c": "mkcoa5530S0",
      "scid": "mkcoa5530",
      "tr_trm_ipadr": "127.0.0.1",
      "tr_brc": "10001",
      "optr_eno": "E0000001",
      "ttl_ug_ync": 0
    }
  },
  "dto": {
    "pageNo": 1,
    "pageSize": 20
  }
}
```

## 등록 거래

| 프로그램 | API |
|---|---|
| **이미지로그 관리** | `/imagelog` (`POST /mkcoa8888S0` 조회 · `POST /mkcoa8888D0` 삭제) |
| `mkcoa5530` | `POST /mkcoa5530S0` (안내항목 목록) |
| `mkcoa9999` | `POST /mkcoa9999S0` (영업팁 실적 목록) |
