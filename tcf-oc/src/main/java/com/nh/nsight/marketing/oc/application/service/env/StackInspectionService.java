package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.support.JvmSizingGuide;
import com.nh.nsight.marketing.oc.support.StackLayerRuleCatalog;
import com.nh.nsight.marketing.oc.support.VmProfile;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;
import com.nh.nsight.marketing.oc.application.dto.env.LayerGridRow;
import com.nh.nsight.marketing.oc.application.dto.env.StackLayerView;
import com.nh.nsight.marketing.oc.application.dto.env.StackSettingRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StackInspectionService {

    private final RuntimeConfigResolver runtimeConfigResolver;

    public StackInspectionService(RuntimeConfigResolver runtimeConfigResolver) {
        this.runtimeConfigResolver = runtimeConfigResolver;
    }

    public List<LayerGridRow> buildLayerGrid(CapacityPlannerRequest request, int timeoutSec, int sessionMin) {
        VmProfile vm = resolveVmProfile(request);
        int cores = effectiveCores(request, vm);
        int memoryGb = effectiveMemoryGb(request, vm);
        return StackLayerRuleCatalog.buildGrid(
                runtimeConfigResolver.resolveAll(), sessionMin, timeoutSec, vm, cores, memoryGb);
    }

    /** ENV-002·CUSTOM 입력 코어·RAM (JVM·Grid 산정용). */
    public static int effectiveCores(CapacityPlannerRequest request, VmProfile vm) {
        if (request.customVm() && request.customCore() > 0) {
            return request.customCore();
        }
        return vm.getCores();
    }

    public static int effectiveMemoryGb(CapacityPlannerRequest request, VmProfile vm) {
        if (request.customVm() && request.customMemoryGb() > 0) {
            return request.customMemoryGb();
        }
        if (request.customVm() && request.customCore() > 0) {
            return request.customCore() * JvmSizingGuide.GB_PER_CORE_IAAS;
        }
        return vm.getMemoryGb();
    }

    public List<StackLayerView> buildStackLayers(List<LayerGridRow> grid) {
        Map<String, List<LayerGridRow>> byLayer = new LinkedHashMap<>();
        for (LayerGridRow row : grid) {
            byLayer.computeIfAbsent(row.layer(), k -> new ArrayList<>()).add(row);
        }
        List<StackLayerView> layers = new ArrayList<>();
        int order = 1;
        for (Map.Entry<String, List<LayerGridRow>> e : byLayer.entrySet()) {
            List<StackSettingRow> settings = e.getValue().stream()
                    .map(this::toSettingRow)
                    .toList();
            boolean valid = settings.stream().noneMatch(s -> "CRITICAL".equals(s.status()));
            layers.add(new StackLayerView(order++, layerId(e.getKey()), e.getKey(),
                    layerDesc(e.getKey()), valid, settings));
        }
        return layers;
    }

    public VmProfile resolveVmProfile(CapacityPlannerRequest request) {
        if (request.customVm() && request.customCore() > 0) {
            int cores = request.customCore();
            int mem = request.customMemoryGb() > 0
                    ? request.customMemoryGb()
                    : cores * JvmSizingGuide.GB_PER_CORE_IAAS;
            return VmProfile.find(request.vmProfileId()).orElseGet(() -> VmProfile.nearest(cores, mem));
        }
        return VmProfile.find(request.vmProfileId()).orElse(VmProfile.defaultProfile());
    }

    private StackSettingRow toSettingRow(LayerGridRow g) {
        return new StackSettingRow(
                g.settingLabel(),
                g.propertyKey(),
                g.configLocation(),
                g.currentValue(),
                g.recommendedValue(),
                g.status(),
                g.statusLabel(),
                g.reason(),
                g.settingExample(),
                g.actionGuide(),
                null
        );
    }

    private String layerId(String layer) {
        return switch (layer) {
            case "UI" -> "UI";
            case "GSLB" -> "GSLB";
            case "L4" -> "L4";
            case "Apache" -> "APACHE";
            case "Tomcat" -> "TOMCAT";
            case "JVM" -> "JVM";
            case "Spring Boot" -> "SPRINGBOOT";
            case "MyBatis" -> "MYBATIS";
            default -> layer.toUpperCase();
        };
    }

    private String layerDesc(String layer) {
        return switch (layer) {
            case "UI" -> "WebTopSuite · 단말/채널";
            case "GSLB" -> "DNS TTL · Health · 센터 라우팅";
            case "L4" -> "Sticky · Health · Idle · LB 방식 · MaxConn";
            case "Apache" -> "Proxy · KeepAlive";
            case "Tomcat" -> "maxThreads · Pool · KeepAlive · Xss";
            case "JVM" -> "Heap · G1GC · Metaspace · OOM Dump";
            case "Spring Boot" -> "Tomcat · Session · Async · TX";
            case "MyBatis" -> "SQL Timeout · Fetch";
            default -> layer;
        };
    }
}
