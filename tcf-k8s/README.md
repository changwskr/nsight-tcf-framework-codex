# tcf-k8s

NSIGHT TCF Framework Kubernetes 배포 자산.

## 빠른 배포

```bash
cd tcf-k8s/scripts
chmod +x deploy.sh
./deploy.sh development <commit-sha>
./deploy.sh production  <commit-sha> eb,sv,ui
```

Windows (Git Bash 필요):

```bat
deploy.bat development abc1234
```

## 환경 매핑

| argument     | Spring profile | replicas | resources (req → lim)   |
|--------------|----------------|----------|-------------------------|
| development  | local          | 1        | 100m/256Mi → 200m/512Mi |
| staging      | dev            | 2        | 200m/512Mi → 500m/1Gi   |
| production   | prod           | 3        | 500m/1Gi → 1000m/2Gi    |

## 이미지 / 네이밍

- Image: `ghcr.io/changwskr/nsight-tcf-framework/<module-service>:<commit-sha>`
  - 예: `.../eb-service:abc1234`, `.../tcf-ui:abc1234`
- Namespace: `nsight` (`NAMESPACE` 로 변경 가능)
- Deployment: `nsight-<module>` (예: `nsight-eb`)
- Container: `<module>-service` / `tcf-ui` / `tcf-gateway`

Deployment가 아직 없으면 해당 모듈은 skip 합니다.  
먼저 `eb-service/scripts/k8s/` 등 매니페스트를 apply 하세요.

## 구조

```
tcf-k8s/
  scripts/deploy.sh|bat
  templates/ingress.yaml
  configs/<module>-secret.yaml   # optional
```
