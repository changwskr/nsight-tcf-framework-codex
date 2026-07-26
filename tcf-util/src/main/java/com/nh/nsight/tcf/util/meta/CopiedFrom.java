package com.nh.nsight.tcf.util.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * tcf-util에 복사·통합된 유틸리티의 원본 출처를 표시한다.
 * <p>
 * {@link #nativeUtility()} 가 {@code true}이면 tcf-util 최초 정의(native)이다.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CopiedFrom {

    /** 원본 Gradle 모듈명 (예: {@code tcf-gateway}). */
    String module();

    /** 원본 클래스 FQCN 또는 simple name. */
    String sourceClass();

    UtilCategory category();

    /** tcf-util 최초 정의 여부 (다른 모듈에서 복사하지 않음). */
    boolean nativeUtility() default false;
}
