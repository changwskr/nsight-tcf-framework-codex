#!/usr/bin/env bash
LOGPATH="../../../../_runs/orchestrator_simulation.log"
WORKSPACE="../_workspace"

mkdir -p "$WORKSPACE"

echo "==== Orchestrator Simulation START: $(date) ==== " > "$LOGPATH"

echo "[$(date '+%F %T')] [Analyst] 수집: 도메인 요구 수집 및 요약" >> "$LOGPATH"
echo "[$(date '+%F %T')] - 도메인: 결제 위험 분석" >> "$LOGPATH"
echo "[$(date '+%F %T')] - 요약: 리스크 항목 1,2,3 식별" >> "$LOGPATH"

echo "Creating artifact: analysis-summary.md"
echo "# 분석 요약" > "$WORKSPACE/analysis-summary.md"
echo "도메인: 결제 위험 분석" >> "$WORKSPACE/analysis-summary.md"
echo "식별: 리스크 항목 1,2,3" >> "$WORKSPACE/analysis-summary.md"
echo "[$(date '+%F %T')] [Analyst] wrote $WORKSPACE/analysis-summary.md" >> "$LOGPATH"

sleep 1

echo "[$(date '+%F %T')] [Builder] 구현: 샘플 스펙을 바탕으로 PoC 생성" >> "$LOGPATH"
echo "Creating artifact: poc-plan.md"
echo "# PoC 계획" > "$WORKSPACE/poc-plan.md"
echo "산출물: 분석-요약.md, poc-plan.md" >> "$WORKSPACE/poc-plan.md"
echo "단계: 1. 설계 2. 구현 3. 검증" >> "$WORKSPACE/poc-plan.md"
echo "[$(date '+%F %T')] [Builder] wrote $WORKSPACE/poc-plan.md" >> "$LOGPATH"

sleep 1

echo "[$(date '+%F %T')] [QA] 검증: PoC 검증 및 결과 보고" >> "$LOGPATH"
RESULT=PASS
if [ ! -f "$WORKSPACE/analysis-summary.md" ] || [ ! -f "$WORKSPACE/poc-plan.md" ]; then
	RESULT=FAIL
fi
if [ "$RESULT" = "PASS" ]; then
	echo "[$(date '+%F %T')] [QA] 결과: 통과" >> "$LOGPATH"
	echo "[$(date '+%F %T')] [QA] note: 경고(사용자 주의)" >> "$LOGPATH"
else
	echo "[$(date '+%F %T')] [QA] 결과: 실패 - 아티팩트 누락" >> "$LOGPATH"
fi

echo "==== Orchestrator Simulation END: $(date) ==== " >> "$LOGPATH"

echo "Simulation completed. Log: $LOGPATH"
