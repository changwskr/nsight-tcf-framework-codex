package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.support.DbPoolSizingGuide;
import com.nh.nsight.marketing.oc.support.JvmSizingGuide;
import com.nh.nsight.marketing.oc.support.TomcatWasSizingGuide;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.VmProfile;
import com.nh.nsight.marketing.oc.application.dto.env.JvmSizingRecommendation;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerResult;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityTimeoutMatrixRow;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityVmResultRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CapacityPlannerService {

    public CapacityPlannerResult plan(CapacityPlannerRequest request) {
        var coreTps = NsightCapacityDerivation.coreTpsFromTpmc(request.tpmcPerTps());
        VmContext ctx = VmContext.from(request, coreTps);
        JvmSizingRecommendation jvm = JvmSizingGuide.recommend(ctx.cores, ctx.memoryGb, ctx.profileId);
        String jvmHeapPerVm = jvm.heapGeneralMinGb() + "~" + jvm.heapGeneralMaxGb() + " GB";
        String jvmHeapSvPerVm = "≤" + jvm.heapSingleViewMaxGb() + " GB";
        VmProfile sizingProfile = ctx.profile != null ? ctx.profile : VmProfile.nearest(ctx.cores, ctx.memoryGb);
        var thSpec = sizingProfile.getTomcatHikariSpec();
        String wasThreadsPerVm = thSpec.tomcatMaxThreadsRange();
        String tomcatBusyFormula = thSpec.busyThreadFormula();
        String tomcatSizingNote = TomcatWasSizingGuide.maxThreadsRationale(sizingProfile);
        String jvmHeapDerivation = JvmSizingGuide.buildDerivationFormula(ctx.cores, ctx.memoryGb, ctx.profileId);
        String wasThreadsDerivation = TomcatWasSizingGuide.buildDerivationFormula(sizingProfile);
        var dbPoolSizing = DbPoolSizingGuide.recommend(sizingProfile, ctx.cores, ctx.memoryGb);
        int dbPoolPerVm = request.hikariPoolPerVm() > 0
                ? request.hikariPoolPerVm()
                : dbPoolSizing.recommendedGeneral();
        int designSessions = (int) Math.ceil(request.totalUsers() * 1.3);
        int sessionMin = request.sessionIdleMinutes().stream().min(Integer::compareTo).orElse(60);

        List<CapacityVmResultRow> vmResults = new ArrayList<>();
        List<CapacityTimeoutMatrixRow> matrix = new ArrayList<>();

        for (int percent : sorted(request.actualRequestPercents())) {
            int users = NsightCapacityDerivation.expectedActualRequestFromPercent(request.totalUsers(), percent);
            for (int timeoutSec : sorted(request.responseTimeoutSeconds())) {
                int tps = NsightCapacityDerivation.peakTpsFromActualRequestUsers(users, timeoutSec * 1000);
                long tpmc = (long) tps * request.tpmcPerTps();
                int vmSingle = (int) Math.ceil((double) tps / Math.max(1, ctx.vmTpsBase));
                int vmAa = request.activeActive() ? vmSingle * 2 : vmSingle;
                long dbPool = (long) vmAa * dbPoolPerVm;
                String status = tpsStatus(tps, ctx, request);
                String reason = tpsReason(tps, ctx, vmSingle, request, dbPool);

                vmResults.add(new CapacityVmResultRow(
                        percent, timeoutSec, users, tps, tpmc,
                        ctx.displayLabel(), ctx.vmTpsBase, vmSingle, vmAa,
                        jvmHeapPerVm, jvmHeapSvPerVm, wasThreadsPerVm, dbPoolPerVm,
                        dbPoolSizing.rangeLabel(), dbPoolSizing.formulaSummary(), dbPool,
                        status, reason
                ));
                matrix.add(new CapacityTimeoutMatrixRow(percent, users, timeoutSec, tps, tpmc));
            }
        }

        Map<String, Integer> risk = new LinkedHashMap<>();
        risk.put("normal", (int) vmResults.stream().filter(r -> "NORMAL".equals(r.status())).count());
        risk.put("warning", (int) vmResults.stream().filter(r -> "WARN".equals(r.status())).count());
        risk.put("critical", (int) vmResults.stream().filter(r -> "CRITICAL".equals(r.status())).count());

        return new CapacityPlannerResult(
                request.totalUsers() + "명 / " + ctx.displayLabel() + " / 세션 " + sessionMin + "분",
                request.branchCount(),
                request.usersPerBranch(),
                request.totalUsers(),
                designSessions,
                ctx.profileId,
                ctx.cores,
                ctx.memoryGb,
                ctx.tps30,
                ctx.tps35,
                ctx.tps40,
                request.tpsPerCoreMin(),
                request.tpsPerCoreBase(),
                request.tpsPerCoreMax(),
                request.tpmcPerTps(),
                coreTps.coreTpmcPerSec(),
                !request.manualCoreTps(),
                sessionMin,
                request.activeActive(),
                dbPoolPerVm,
                dbPoolSizing.rangeLabel(),
                dbPoolSizing.formulaSummary(),
                dbPoolSizing.derivationFormula(),
                dbPoolSizing.maxSingleView(),
                dbPoolSizing.minSingleView(),
                jvmHeapDerivation,
                thSpec.tomcatMaxThreadsRange(),
                tomcatBusyFormula,
                tomcatSizingNote,
                wasThreadsDerivation,
                thSpec.minSpareThreadsRange(),
                thSpec.acceptCountRange(),
                thSpec.maxConnectionsRange(),
                thSpec.hikariSingleViewRange(),
                thSpec.cautionNote(),
                TomcatWasSizingGuide.OPERATIONAL_NOTE,
                request.dbSessionLimit(),
                vmResults,
                matrix,
                risk,
                buildSummaryFormula(request, ctx, coreTps)
        );
    }

    private String tpsStatus(int tps, VmContext ctx, CapacityPlannerRequest request) {
        if (tps > ctx.tps40) {
            return "CRITICAL";
        }
        if (tps > ctx.vmTpsBase * 0.8) {
            return "WARN";
        }
        return "NORMAL";
    }

    private String tpsReason(int tps, VmContext ctx, int vmSingle, CapacityPlannerRequest request, long dbPool) {
        if (request.validateDbPool() && dbPool > request.dbSessionLimit()) {
            return "DB Pool 총량 " + dbPool + " > 한도 " + request.dbSessionLimit();
        }
        if (request.drValidation() && request.activeActive() && vmSingle < 2 && tps > ctx.vmTpsBase / 2) {
            return "A-A: 센터당 VM 2대 이상 검토";
        }
        return tps > ctx.tps40 ? "VM 최대 TPS(" + ctx.tps40 + ") 초과" : "처리량 범위 내";
    }

    private List<Integer> sorted(List<Integer> values) {
        return values.stream().sorted().distinct().toList();
    }

    private static String buildSummaryFormula(
            CapacityPlannerRequest request,
            VmContext ctx,
            NsightCapacityDerivation.CoreTpsFromTpmc coreTps) {
        return "실요청자=전체×% · 목표TPS=실요청자÷Timeout · 전사TPMC=목표TPS×"
                + request.tpmcPerTps()
                + " · VM TPS(기준)=" + ctx.cores + "×" + request.tpsPerCoreBase() + "=" + ctx.vmTpsBase
                + " · 필요VM=ceil(목표TPS÷" + ctx.vmTpsBase + ")"
                + (request.activeActive() ? " · A-A=×2" : "");
    }

    private record VmContext(
            String profileId,
            VmProfile profile,
            int cores,
            int memoryGb,
            int tps30,
            int tps35,
            int tps40,
            int vmTpsBase
    ) {
        String displayLabel() {
            return cores + "코어/" + memoryGb + "GB (" + profileId + ")";
        }

        static VmContext from(CapacityPlannerRequest request, NsightCapacityDerivation.CoreTpsFromTpmc coreTps) {
            int min = request.tpsPerCoreMin();
            int base = request.tpsPerCoreBase();
            int max = request.tpsPerCoreMax();
            if (request.customVm() && request.customCore() > 0) {
                int c = request.customCore();
                int mem = request.customMemoryGb() > 0 ? request.customMemoryGb() : 64;
                String id = c + "CORE-" + mem + "GB";
                return new VmContext(
                        id,
                        null,
                        c,
                        mem,
                        c * min,
                        c * base,
                        c * max,
                        c * base
                );
            }
            VmProfile p = VmProfile.find(request.vmProfileId()).orElse(VmProfile.defaultProfile());
            int c = p.getCores();
            return new VmContext(p.getId(), p, c, p.getMemoryGb(),
                    c * min, c * base, c * max, c * base);
        }
    }
}
