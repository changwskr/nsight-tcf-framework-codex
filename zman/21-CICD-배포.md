# 21장. CI/CD / 배포 구조 — 설명

## 설계서 절 목차

21.1~21.3 개요 · 21.4~21.8 Pipeline · 21.9~21.15 WAR·Tomcat · 21.16~21.25 Health·Rollback·OM · 21.26~21.27 운영

---

## 핵심 결론

```
GitLab → MR → Runner → Gradle(Test) → WAR → Artifact
→ 승인 → Deploy → Health → Traffic → OM 배포이력
```

**OM** = 요청·승인·이력 · **CI/CD** = 빌드·배포·Rollback 실행

---

## Pipeline (21.4~21.8)

| 단계 | 도구 |
|------|------|
| 형상 | GitLab, MR, Review |
| 빌드 | GitLab Runner, Gradle bootWar |
| 품질 | Unit Test, SonarQube (목표) |
| 산출물 | Nexus/Artifact Repo |
| 배포 | tcf-scripts, tcf-cicd |
| 검증 | Actuator health, Smoke |

## 배포 모드 (21.9~21.15)

| 모드 | 설명 |
|------|------|
| bootRun | 개발 |
| ztomcat | Tomcat 8080, **13 WAR** 검증 (`deploy-wars.sh`) |
| 운영 | 17 WAR (목표), Apache VIP → Tomcat Cluster |

WAR: `WEB-INF/lib` tcf-* JAR 내장.

## 배포 절차 (21.x)

1. Apache drain (선택)  
2. WAR 교체  
3. Context restart  
4. Health Check  
5. Traffic restore  
6. OM_DEPLOY_* 기록  

## Rollback

- 이전 artifact 재배포  
- OM 롤백 이력·사유  

## OM 연계 (21.x)

- `OmDeployHandler` — 요청·승인·상태  
- CI/CD는 **실행**, OM은 **통제·기록**

## 현재 vs 목표 (Gap)

| | develop | 운영 목표 |
|---|---------|-----------|
| CI | tcf-cicd, scripts | GitLab Pipeline |
| 품질 Gate | 부분 | SonarQube 필수 |
| 17 WAR | 9 WAR | 확장 |

## 코드베이스

- `tcf-cicd/`  
- `tcf-scripts/`  
- `ztomcat/`  
- `scripts/`

## 운영 체크리스트

- [ ] Health 실패 시 auto-rollback  
- [ ] WAR별 독립 배포 가능  
- [ ] 배포 감사로그  

## 이전 · 다음

← [20장 Spring](./20-Spring환경설정.md) · [22장 샘플](./22-업무서비스샘플.md) →
