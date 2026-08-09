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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "framework.message")
public class MessageProperties {

    private boolean enabled = false;
    private String dataSource;
    private String[] tableName;
}
