package com.nh.nsight.marketing.oc.capnew.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewDefaultsCDTO;
import com.nh.nsight.marketing.oc.capnew.persistence.dao.CapNewScenarioTemplateDao;
import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioTemplateRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CapNewTemplateDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CapNewTemplateDataInitializer.class);

    private final CapNewScenarioTemplateDao templateDao;
    private final CapNewDefaultsService defaultsService;
    private final CapNewTemplateSeedFactory seedFactory;
    private final ObjectMapper objectMapper;

    public CapNewTemplateDataInitializer(
            CapNewScenarioTemplateDao templateDao,
            CapNewDefaultsService defaultsService,
            CapNewTemplateSeedFactory seedFactory,
            ObjectMapper objectMapper) {
        this.templateDao = templateDao;
        this.defaultsService = defaultsService;
        this.seedFactory = seedFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        CapNewDefaultsCDTO defaults = defaultsService.defaults();
        for (CapNewTemplateSeedFactory.TemplateCatalog catalog : seedFactory.catalogs()) {
            try {
                CapNewScenarioTemplateRow row = toRow(catalog, seedFactory.buildSeed(catalog.code(), defaults));
                CapNewScenarioTemplateRow existing = templateDao.findByCode(catalog.code());
                if (existing == null) {
                    templateDao.insert(row);
                    log.info("cap-new template seeded: {}", catalog.code());
                } else {
                    row.setEnabled(existing.getEnabled());
                    row.setSortOrder(existing.getSortOrder());
                    templateDao.update(row);
                    log.debug("cap-new builtin template refreshed: {}", catalog.code());
                }
            } catch (Exception ex) {
                log.warn("cap-new template seed skipped ({}): {}", catalog.code(), ex.getMessage());
            }
        }
    }

    private CapNewScenarioTemplateRow toRow(
            CapNewTemplateSeedFactory.TemplateCatalog catalog,
            java.util.Map<String, Object> seed) throws JsonProcessingException {
        CapNewScenarioTemplateRow row = new CapNewScenarioTemplateRow();
        row.setTemplateCode(catalog.code());
        row.setLabel(catalog.label());
        row.setDescription(catalog.description());
        row.setPurpose(catalog.purpose());
        row.setTargetEnv(catalog.targetEnv());
        row.setVmProfileCode(catalog.vmProfileCode());
        row.setTotalUsers(catalog.totalUsers());
        row.setDesignPeakTps(catalog.designPeakTps());
        row.setDeploymentAp(catalog.deploymentAp());
        row.setMaxThreads(catalog.maxThreads());
        row.setPoolPerVm(catalog.poolPerVm());
        row.setSortOrder(catalog.sortOrder());
        row.setEnabled("Y");
        row.setSeedPayload(objectMapper.writeValueAsString(seed));
        return row;
    }
}
