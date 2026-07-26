package com.nh.nsight.tcf.uj.support;

import java.util.List;

public final class BusinessModuleDefinitions {
    private BusinessModuleDefinitions() {}

    public static final List<ModuleDefinition> ALL = List.of(
            new ModuleDefinition("CC", "Common", "공통", 8081),
            new ModuleDefinition("IC", "Integration Customer", "고객", 8082),
            new ModuleDefinition("PC", "Private Customer", "고객", 8083),
            new ModuleDefinition("BC", "Business Customer", "고객", 8084),
            new ModuleDefinition("MS", "Mini Single View", "고객", 8085),
            new ModuleDefinition("SV", "Single View", "마케팅", 8086),
            new ModuleDefinition("PD", "Product", "마케팅", 8087),
            new ModuleDefinition("CM", "Campaign", "마케팅", 8088),
            new ModuleDefinition("EB", "EBM", "마케팅", 8089),
            new ModuleDefinition("EP", "Event Processing", "실시간", 8090),
            new ModuleDefinition("BP", "Behavior Processing", "실시간", 8091),
            new ModuleDefinition("BD", "Behavior Data", "데이터", 8092),
            new ModuleDefinition("SS", "Sales Support", "지원", 8093),
            new ModuleDefinition("CS", "Common Service", "지원", 8094),
            new ModuleDefinition("CT", "Contents", "지원", 8095),
            new ModuleDefinition("MG", "Message", "지원", 8096),
            new ModuleDefinition("OM", "Operation Management (tcf-om)", "운영", 8097),
            new ModuleDefinition("UD", "Common UpDownload (tcf-om)", "공통", 8097),
            new ModuleDefinition("JWT", "JWT Auth (tcf-jwt)", "인증", 8110)
    );

    public record ModuleDefinition(String code, String name, String group, int localPort) {}
}
