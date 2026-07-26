package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.config.EnvCheckProperties;
import com.nh.nsight.marketing.oc.support.JvmSizingGuide;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityDesignView;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;
import com.nh.nsight.marketing.oc.application.dto.env.JvmSizingRecommendation;
import com.nh.nsight.marketing.oc.application.dto.env.LayerGridRow;
import com.nh.nsight.marketing.oc.application.dto.env.StackLayerView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CapacityDesignService {

    private final CapacityPlannerService capacityPlannerService;
    private final StackInspectionService stackInspectionService;
    private final EnvCheckProperties envCheckProperties;

    public CapacityDesignService(
            CapacityPlannerService capacityPlannerService,
            StackInspectionService stackInspectionService,
            EnvCheckProperties envCheckProperties
    ) {
        this.capacityPlannerService = capacityPlannerService;
        this.stackInspectionService = stackInspectionService;
        this.envCheckProperties = envCheckProperties;
    }

    public CapacityPlannerRequest defaultRequest() {
        int branches = envCheckProperties.getBranchCount() > 0
                ? envCheckProperties.getBranchCount() : 3600;
        int perBranch = envCheckProperties.getUsersPerBranch() > 0
                ? envCheckProperties.getUsersPerBranch() : 6;
        return new CapacityPlannerRequest(
                "기본 시나리오",
                branches,
                perBranch,
                envCheckProperties.getTotalUsers(),
                "8CORE-64GB",
                false,
                0,
                0,
                30,
                35,
                40,
                3000,
                false,
                List.of(3, 5, 10, 15),
                List.of(3, 4, 5),
                List.of(60, 90),
                true,
                true,
                true,
                true,
                0,
                envCheckProperties.getDbSessionLimit()
        );
    }

    public CapacityDesignView analyze(CapacityPlannerRequest request) {
        int timeoutSec = request.responseTimeoutSeconds().stream().min(Integer::compareTo).orElse(3);
        int sessionMin = request.sessionIdleMinutes().stream().min(Integer::compareTo).orElse(60);
        var planner = capacityPlannerService.plan(request);
        List<LayerGridRow> grid = stackInspectionService.buildLayerGrid(request, timeoutSec, sessionMin);
        List<StackLayerView> layers = stackInspectionService.buildStackLayers(grid);
        JvmSizingRecommendation jvmSizing = JvmSizingGuide.recommend(
                planner.vmCores(), planner.vmMemoryGb(), planner.vmProfileId());
        boolean stackValid = grid.stream().noneMatch(r -> "CRITICAL".equals(r.status()));
        String scenarioId = "SCN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return new CapacityDesignView(scenarioId, planner, layers, grid, jvmSizing, stackValid, timeoutSec, sessionMin);
    }
}
