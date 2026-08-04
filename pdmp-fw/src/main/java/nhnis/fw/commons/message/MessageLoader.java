/*
 * Copyright 2026 by Nonghyup. All rights reserved. Nonghyup 의 사전 승인 없이
 * 본 내용의 전부 또는 일부에 대한 복사, 배포, 사용을 금합니다. Nonghyup의 사전 승인
 * 없이 소스코드를 변경하여 사용하는 경우 소스코드에 대한 품질과 성능을 보장하지 않습니다.
 *
 * If you modify this source without Nonghyup's approval. Nonghyup does
 * not guarantee the quality and performance of source.
 */
/*
 * 이 력 사 항
 *
 * 일 자 버 전 작성자 변경사항
 *
 * 2026. 7. 21. 1.0 홍길동 최초작성
 */
package nhnis.fw.commons.message;

import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <PRE>
 * 설명
 *
 * </PRE>
 *
 * @author 홍길동
 * @version 1.0, 2026. 7. 21.
 * @logicalName
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageLoader {

    private final ApplicationContext applicationContext;
    private final MessageProperties properties;
    private final MessageCache messageCache;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("DB Message Bundle Disabled");
            return;
        }

        if (properties.getDataSource() == null || properties.getDataSource().isBlank()) {
            throw new IllegalArgumentException("Message Bundle 데이터 소스 설정이 없습니다.");
        }

        DataSource dataSource = applicationContext.getBean(
                properties.getDataSource(),
                DataSource.class
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        messageCache.clear();

        String tables[] = properties.getTableName();
        StringBuilder sql = new StringBuilder();

        for (int i = 0; i < tables.length; i++) {
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT CRM_MSG_C, MSG_CNTN FROM ")
                    .append(tables[i]);
        }

        jdbcTemplate.query(
                sql.toString(),
                rs -> {
                    while (rs.next()) {
                        messageCache.put(
                                rs.getString("CRM_MSG_C"),
                                rs.getString("MSG_CNTN")
                        );
                    }
                    return null;
                }
        );

        log.info("DB Message Bundle Loaded: 총 {}건 조회", messageCache.size());
    }
}
