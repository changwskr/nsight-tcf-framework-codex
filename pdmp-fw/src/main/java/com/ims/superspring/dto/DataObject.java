package com.ims.superspring.dto;

import java.io.Serializable;

/**
 * IMS SuperSpring DataObject 스텁.
 * 원본 JAR가 없을 때 MessageInfo / ApiGatewayDto 등 생성 DTO 컴파일용.
 * 사내 SuperSpring 의존성이 있으면 이 클래스를 제거하고 원본을 사용한다.
 */
public abstract class DataObject implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @Override
    public abstract Object clone();
}
