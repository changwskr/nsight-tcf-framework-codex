package com.nh.nsight.gateway.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class GatewayRouteSchemaInitializer {
    private final DataSource dataSource;
    private final GatewayRouteLocalCatalogSeeder catalogSeeder;

    public GatewayRouteSchemaInitializer(
            @Qualifier("gatewayRouteDataSource") DataSource dataSource,
            GatewayRouteLocalCatalogSeeder catalogSeeder) {
        this.dataSource = dataSource;
        this.catalogSeeder = catalogSeeder;
    }

    @PostConstruct
    public void initialize() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.addScript(new ClassPathResource("data.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
        catalogSeeder.seedMissingRoutes();
    }
}
