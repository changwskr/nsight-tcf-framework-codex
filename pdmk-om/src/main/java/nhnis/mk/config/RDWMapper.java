package nhnis.mk.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.annotations.Mapper;

/**
 * RDW MyBatis Mapper 표기.
 *
 * <p>운영 생성 코드의 {@code @RDWMapper}와 동일하다. 메타로 {@link Mapper}를 포함한다.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Mapper
public @interface RDWMapper {
}
