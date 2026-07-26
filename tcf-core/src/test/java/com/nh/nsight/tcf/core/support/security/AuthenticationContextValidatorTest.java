package com.nh.nsight.tcf.core.support.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationContextValidatorTest {
    private final TcfProperties properties = new TcfProperties();
    private final AuthenticationContextValidator validator = new AuthenticationContextValidator(properties);

    @BeforeEach
    void setUp() {
        properties.setAuthenticationContextValidationEnabled(true);
    }

    @AfterEach
    void tearDown() {
        AuthenticationContextHolder.clear();
    }

    @Test
    void skipsWhenNoAuthenticationContext() {
        StandardHeader header = header("user01", "001", "WEB");
        TransactionContext context = new TransactionContext(header);
        assertDoesNotThrow(() -> validator.validate(header, context));
    }

    @Test
    void passesWhenClaimsMatchHeader() {
        AuthenticationContextHolder.set(new AuthenticationContext("user01", "001", "WEB", "jti-1"));
        StandardHeader header = header("user01", "001", "WEB");
        TransactionContext context = new TransactionContext(header);
        validator.validate(header, context);
        assertEquals("jti-1", context.get("jwtJti"));
    }

    @Test
    void failsWhenUserIdMismatch() {
        AuthenticationContextHolder.set(new AuthenticationContext("user01", "001", "WEB", "jti-1"));
        StandardHeader header = header("other", "001", "WEB");
        TransactionContext context = new TransactionContext(header);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validate(header, context));
        assertEquals(ErrorCode.JWT_HEADER_CLAIM_MISMATCH, ex.getErrorCode());
    }

    private static StandardHeader header(String userId, String branchId, String channelId) {
        StandardHeader header = new StandardHeader();
        header.setUserId(userId);
        header.setBranchId(branchId);
        header.setChannelId(channelId);
        return header;
    }
}
