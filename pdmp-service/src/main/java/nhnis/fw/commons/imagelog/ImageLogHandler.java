/****************************************************************************
 * Copyright 2026 by Nonghyup. All rights reserved. Nonghyup 의 사전 승인 없이
 * 본 내용의 전부 또는 일부에 대한 복사, 배포, 사용을 금합니다. Nonghyup의 사전 승인
 * 없이 소스코드를 변경하여 사용하는 경우 소스코드에 대한 품질과 성능을 보장하지 않습니다.
 *
 * If you modify this source without Nonghyup’s approval. Nonghyup does
 * not guarantee the quality and performance of source.
 ****************************************************************************/
/****************************************************************************
 * 이 력 사 항
 ****************************************************************************
 * 일 자 버 전 작 성 자 변경사항
 ****************************************************************************
 * 2026. 7. 6. 1.0 CS549257 최초작성
 ****************************************************************************/
package nhnis.fw.commons.imagelog;

import org.springframework.stereotype.Component;

import nhnis.fw.commons.dto.header.hdr_nhnis;

/**
 * <PRE>
 *
 * 이미지로그 핸들러
 * </PRE>
 *
 * @author CS549257
 * @version 1.0, 2026. 7. 6.
 * @logicalName
 */
@Component
public class ImageLogHandler {

    public void preImagelog(hdr_nhnis header, Object inputDto) {
    }

    public void postImagelog(hdr_nhnis header, Object inputDto) {
    }

    public void exceptionImagelog(
            hdr_nhnis header,
            Object inputDto,
            Throwable t
    ) {
    }

    private void insertImagelog(
            hdr_nhnis header,
            Object inputDto,
            Throwable t
    ) {
        if (t != null) {

        } else {

        }
    }
}
