package com.nh.nsight.tcf.util.crypto;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * tcf-jwt {@code JwtSupport.sha256Hex} 복사본.
 */
@CopiedFrom(module = "tcf-jwt", sourceClass = "JwtSupport", category = UtilCategory.CRYPTO)
public final class JwtHashUtils implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-jwt";
    public static final String COPIED_FROM_CLASS = "JwtSupport";

    private JwtHashUtils() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
