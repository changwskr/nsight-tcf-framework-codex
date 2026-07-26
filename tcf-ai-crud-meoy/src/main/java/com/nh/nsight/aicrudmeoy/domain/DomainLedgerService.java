package com.nh.nsight.aicrudmeoy.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.aicrudmeoy.config.CrudMeoyProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DomainLedgerService {

    private final CrudMeoyProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private DomainLedgerRoot ledger;

    public DomainLedgerService(
            CrudMeoyProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() throws IOException {
        Resource resource = resourceLoader.getResource(properties.getDomainLedgerResource());
        try (InputStream in = resource.getInputStream()) {
            ledger = objectMapper.readValue(in, DomainLedgerRoot.class);
        }
    }

    public DomainLedgerRoot getRoot() {
        return ledger;
    }

    public Map<String, Object> summary() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", ledger.getVersion());
        body.put("generatedAt", ledger.getGeneratedAt());
        body.put("sourceNote", ledger.getSourceNote());
        body.put("moduleCount", ledger.getModuleCount());
        body.put("domainCount", ledger.getDomainCount());
        body.put("serviceIdCount", ledger.getServiceIdCount());
        body.put("groups", ledger.getModules().stream()
                .map(BusinessModuleLedger::getGroup)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList());
        body.put("statuses", ledger.getModules().stream()
                .map(BusinessModuleLedger::getStatus)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList());
        return body;
    }

    public List<BusinessModuleLedger> modules() {
        return ledger.getModules();
    }

    public BusinessModuleLedger requireModule(String businessCode) {
        String code = businessCode == null ? "" : businessCode.trim().toUpperCase(Locale.ROOT);
        return ledger.getModules().stream()
                .filter(m -> code.equalsIgnoreCase(m.getBusinessCode()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "업무코드를 찾을 수 없습니다: " + businessCode));
    }

    public Map<String, Object> search(String q, String group, String status, String businessCode) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String groupFilter = group == null ? "" : group.trim();
        String statusFilter = status == null ? "" : status.trim();
        String bcFilter = businessCode == null ? "" : businessCode.trim();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (BusinessModuleLedger module : ledger.getModules()) {
            if (!groupFilter.isBlank() && !groupFilter.equals(module.getGroup())) {
                continue;
            }
            if (!statusFilter.isBlank() && !statusFilter.equalsIgnoreCase(module.getStatus())) {
                continue;
            }
            if (!bcFilter.isBlank() && !bcFilter.equalsIgnoreCase(module.getBusinessCode())) {
                continue;
            }
            if (module.getDomains() == null || module.getDomains().isEmpty()) {
                if (query.isBlank() || matchesModule(module, query)) {
                    rows.add(moduleRow(module, null, null));
                }
                continue;
            }
            for (DomainLedgerItem domain : module.getDomains()) {
                if (domain.getServiceIds() == null || domain.getServiceIds().isEmpty()) {
                    if (query.isBlank() || matchesDomain(module, domain, null, query)) {
                        rows.add(moduleRow(module, domain, null));
                    }
                    continue;
                }
                for (ServiceIdLedgerItem sid : domain.getServiceIds()) {
                    if (query.isBlank() || matchesDomain(module, domain, sid, query)) {
                        rows.add(moduleRow(module, domain, sid));
                    }
                }
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", rows.size());
        body.put("rows", rows);
        body.put("modules", filterModules(query, groupFilter, statusFilter, bcFilter));
        return body;
    }

    private List<BusinessModuleLedger> filterModules(
            String query, String groupFilter, String statusFilter, String bcFilter) {
        return ledger.getModules().stream()
                .filter(m -> groupFilter.isBlank() || groupFilter.equals(m.getGroup()))
                .filter(m -> statusFilter.isBlank() || statusFilter.equalsIgnoreCase(m.getStatus()))
                .filter(m -> bcFilter.isBlank() || bcFilter.equalsIgnoreCase(m.getBusinessCode()))
                .filter(m -> query.isBlank() || moduleContains(m, query))
                .collect(Collectors.toList());
    }

    private boolean moduleContains(BusinessModuleLedger module, String query) {
        if (matchesModule(module, query)) {
            return true;
        }
        if (module.getDomains() == null) {
            return false;
        }
        for (DomainLedgerItem d : module.getDomains()) {
            if (matchesDomain(module, d, null, query)) {
                return true;
            }
            if (d.getServiceIds() == null) {
                continue;
            }
            for (ServiceIdLedgerItem s : d.getServiceIds()) {
                if (matchesDomain(module, d, s, query)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesModule(BusinessModuleLedger module, String query) {
        if (query.isBlank()) {
            return true;
        }
        return contains(module.getBusinessCode(), query)
                || contains(module.getModuleName(), query)
                || contains(module.getGradleModule(), query)
                || contains(module.getGroup(), query)
                || contains(module.getStatus(), query);
    }

    private boolean matchesDomain(
            BusinessModuleLedger module,
            DomainLedgerItem domain,
            ServiceIdLedgerItem sid,
            String query) {
        if (matchesModule(module, query)) {
            return true;
        }
        if (contains(domain.getDomainCode(), query) || contains(domain.getDomainName(), query)
                || contains(domain.getHandler(), query)) {
            return true;
        }
        return sid != null && (contains(sid.getServiceId(), query)
                || contains(sid.getAction(), query)
                || contains(sid.getOperation(), query));
    }

    private Map<String, Object> moduleRow(
            BusinessModuleLedger module, DomainLedgerItem domain, ServiceIdLedgerItem sid) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessCode", module.getBusinessCode());
        row.put("moduleName", module.getModuleName());
        row.put("group", module.getGroup());
        row.put("localPort", module.getLocalPort());
        row.put("gradleModule", module.getGradleModule());
        row.put("status", module.getStatus());
        if (domain != null) {
            row.put("domainCode", domain.getDomainCode());
            row.put("domainName", domain.getDomainName());
            row.put("handler", domain.getHandler());
        }
        if (sid != null) {
            row.put("serviceId", sid.getServiceId());
            row.put("action", sid.getAction());
            row.put("operation", sid.getOperation());
        }
        return row;
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
