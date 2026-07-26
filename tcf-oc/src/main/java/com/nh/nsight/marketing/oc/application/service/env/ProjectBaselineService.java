package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.config.EnvCheckProperties;
import com.nh.nsight.marketing.oc.support.Nsight32Core256GbGuide;
import com.nh.nsight.marketing.oc.application.dto.env.ProjectBaselineView;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProjectBaselineService {

    private final EnvCheckProperties properties;

    public ProjectBaselineService(EnvCheckProperties properties) {
        this.properties = properties;
    }

    public ProjectBaselineView loadBaseline(String projectId, String envCode) {
        Map<String, String> deploy = new LinkedHashMap<>();
        deploy.put("용량산정 문서", Nsight32Core256GbGuide.CAPACITY_DOC);
        deploy.put("전체 사용자", Nsight32Core256GbGuide.TOTAL_USERS + "명 (지점 "
                + Nsight32Core256GbGuide.BRANCH_COUNT + " × "
                + Nsight32Core256GbGuide.USERS_PER_BRANCH + "명)");
        deploy.put("실요청 사용자", properties.getActualRequestUsers() + "명 (운영 피크 "
                + properties.getActualRequestPeakPercent() + "% · TPS/Thread 산정)");
        deploy.put("실요청 시나리오", "1,080(3%) / 1,800(5%) / 3,600(10%) / 5,400(15%)");
        deploy.put("세션 설계", Nsight32Core256GbGuide.SESSION_DESIGN_COUNT + " (여유 "
                + Nsight32Core256GbGuide.SESSION_BUFFERED_MIN + "~"
                + Nsight32Core256GbGuide.SESSION_BUFFERED_MAX + ")");
        deploy.put("세션 유지", Nsight32Core256GbGuide.SESSION_IDLE_MINUTES + "분 · 객체 "
                + Nsight32Core256GbGuide.SESSION_SIZE_TARGET_KB + "KB 목표 / "
                + Nsight32Core256GbGuide.SESSION_SIZE_MAX_KB + "KB 이내");
        deploy.put("TPS 시나리오", "360(3%) / 600(5%피크) / 1200(10%) / 1800(15%스트레스)");
        deploy.put("AP 구성", "Active-Active · 운영 최소 2대 · DR 시 1센터 단독 TPS 수용");
        deploy.put("DB 구성", "Active-Standby");
        deploy.put("JVM Heap", "일반 " + Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MIN + "~"
                + Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MAX + "GB / SV "
                + Nsight32Core256GbGuide.JVM_HEAP_SINGLEVIEW_GB_MAX + "GB 이내");
        return new ProjectBaselineView(
                projectId != null && !projectId.isBlank() ? projectId : properties.getDefaultProjectId(),
                properties.getDefaultProjectName(),
                envCode != null && !envCode.isBlank() ? envCode : properties.getDefaultEnvCode(),
                properties.getHardwareProfile(),
                Nsight32Core256GbGuide.CAPACITY_DOC,
                properties.getCenterType(),
                properties.getBranchCount(),
                properties.getUsersPerBranch(),
                properties.getTotalUsers(),
                properties.getActualRequestUsers(),
                properties.getActualRequestPeakPercent(),
                properties.getSessionDesignCount(),
                properties.getSessionBufferedMin(),
                properties.getSessionBufferedMax(),
                properties.getBaseTps(),
                properties.getPeakTps(),
                properties.getHighPeakTps(),
                properties.getStressTps(),
                properties.getVmMaxTps(),
                properties.getPeakConcurrentUsers(),
                properties.getApCount(),
                properties.getApVmSpec(),
                properties.getTargetP95Ms(),
                deploy
        );
    }
}
