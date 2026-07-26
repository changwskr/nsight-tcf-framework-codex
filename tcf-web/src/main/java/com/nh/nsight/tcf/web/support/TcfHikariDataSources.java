package com.nh.nsight.tcf.web.support;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

/** ztomcat WAR 재배포 시 Hikari housekeeper 스레드 누수 방지용 DataSource 생성 */
public final class TcfHikariDataSources {

    private TcfHikariDataSources() {}

    public static HikariDataSource create(
            String driverClassName, String url, String username, String password, String poolName) {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password != null ? password : "")
                .build();
        configureForServletContainer(dataSource, poolName);
        return dataSource;
    }

    public static void configureForServletContainer(HikariDataSource dataSource, String poolName) {
        if (poolName != null && !poolName.isBlank()) {
            dataSource.setPoolName(poolName);
        }
        dataSource.setRegisterMbeans(false);
    }
}
