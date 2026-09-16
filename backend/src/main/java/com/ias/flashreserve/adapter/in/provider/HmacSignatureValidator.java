package com.ias.flashreserve.adapter.in.provider;

import com.ias.flashreserve.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final AppProperties properties;

    public HmacSignatureValidator(AppProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String providedSignature, String rawBody) {
        if (providedSignature == null || providedSignature.isBlank() || rawBody == null) {
            return false;
        }
        String expected = sign(rawBody);
        String normalized = providedSignature.trim().toLowerCase().replaceFirst("^sha256=", "");
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                normalized.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String sign(String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(
                    properties.getProvider().getHmacSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            ));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256", e);
        }
    }
}
