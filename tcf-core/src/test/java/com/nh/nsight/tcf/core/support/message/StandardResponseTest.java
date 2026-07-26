package com.nh.nsight.tcf.core.support.message;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StandardResponseTest {
    @Test
    void successResponseHasS0000() {
        StandardHeader header = new StandardHeader();
        StandardResponse<String> response = StandardResponse.success(header, "OK");
        assertThat(response.getResult().getResultCode()).isEqualTo("S0000");
    }
}
