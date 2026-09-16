package com.ias.flashreserve.adapter.in.provider;

import com.ias.flashreserve.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignatureValidatorTest {

    @Test
    void acceptsMatchingHexSignature() {
        AppProperties properties = new AppProperties();
        properties.getProvider().setHmacSecret("test-secret");
        HmacSignatureValidator validator = new HmacSignatureValidator(properties);
        String body = "{\"eventId\":\"1\"}";
        String signature = validator.sign(body);

        assertThat(validator.isValid(signature, body)).isTrue();
        assertThat(validator.isValid("sha256=" + signature, body)).isTrue();
        assertThat(validator.isValid("deadbeef", body)).isFalse();
        assertThat(validator.isValid(null, body)).isFalse();
    }
}
