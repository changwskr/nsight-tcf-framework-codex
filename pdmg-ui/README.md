# PDMG UI (`pdmg-ui`)

`pdmg-service` 전문 테스트용 로컬 UI입니다. 네이밍은 `pdmg-service/docs/MG-NAMING_CONVENTION.md` 를 따른다.

## 실행

```powershell
# 1) pdmg-service (8080)
cd ..\pdmg-service
.\RUN.bat

# 2) pdmg-ui (8090) — pdmg-service를 HTTP로 중계
cd ..\pdmg-ui
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
      "rms_svc_c": "mgcoa5530S0",
      "scid": "mgcoa5530",
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
| **이미지로그 관리** | `/imagelog` (`POST /mgcoa8888S0` 조회 · `POST /mgcoa8888D0` 삭제) |
| `mgcoa5530` | `POST /mgcoa5530S0` (안내항목 목록) |
| `mgcoa9999` | `POST /mgcoa9999S0` (영업팁 실적 목록) |
