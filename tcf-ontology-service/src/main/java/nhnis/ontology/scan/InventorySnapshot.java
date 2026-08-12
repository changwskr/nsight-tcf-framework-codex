package nhnis.ontology.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventorySnapshot {

    private String generatedAt;
    private Map<String, String> roots = new LinkedHashMap<>();
    private List<ProgramInventory> programs = new ArrayList<>();
    private List<String> uiRoutes = new ArrayList<>();
    private List<String> sampleRequests = new ArrayList<>();
    private List<String> fwHighlights = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    @Getter
    @Setter
    public static class ProgramInventory {
        private String programId;
        private String packageRoot;
        private String handler;
        private String facade;
        private String controller;
        private String service;
        private String dao;
        private String mapperXml;
        private Set<String> serviceIds = new LinkedHashSet<>();
        private Set<String> sqlIds = new LinkedHashSet<>();
    }
}
