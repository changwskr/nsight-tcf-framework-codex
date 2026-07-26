# 04-l4-gslb — 안내

센터 앞단 L4/GSLB 템플릿.  
수치 SoT → [../05-L4-GSLB-셋팅정보.md](../05-L4-GSLB-셋팅정보.md)

## 파일

| 파일 | 역할 |
|------|------|
| [l4-gslb-template.md](./l4-gslb-template.md) | 장비 반영용 짧은 표 |

## 요지

- Idle **120초** · Sticky **70분** (Session Idle 60분보다 길게)  
- Health: `/apache-health` 또는 `/sv/actuator/health`  
- 센터 전환 시 세션 미복제 → 재로그인  

관련: [../10-operations-셋팅정보.md](../10-operations-셋팅정보.md) · [../06-apache-셋팅정보.md](../06-apache-셋팅정보.md)
