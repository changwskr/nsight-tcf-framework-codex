package com.nh.nsight.tcf.util.id;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.util.UUID;

/**
 * tcf-jwt {@code JwtSupport} ID/JTI/RefreshToken 생성 복사본.
 */
@CopiedFrom(module = "tcf-jwt", sourceClass = "JwtSupport", category = UtilCategory.ID)
public final class JwtIdUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-jwt";
    public static final String COPIED_FROM_CLASS = "JwtSupport";

    private JwtIdUtils() {
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String newJti() {
        return UUID.randomUUID().toString();
    }

    public static String newRefreshTokenPlain() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }
}
