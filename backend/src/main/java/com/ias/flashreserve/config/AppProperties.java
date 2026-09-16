package com.ias.flashreserve.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Provider provider = new Provider();
    private final Outbox outbox = new Outbox();
    private final Demo demo = new Demo();

    public Cors getCors() {
        return cors;
    }

    public Provider getProvider() {
        return provider;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public Demo getDemo() {
        return demo;
    }

    public static class Cors {
        private String origins = "http://localhost:4200";

        public String getOrigins() {
            return origins;
        }

        public void setOrigins(String origins) {
            this.origins = origins;
        }

        public String[] originArray() {
            return Arrays.stream(origins.split(","))
                    .map(s -> s.trim())
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);
        }
    }

    public static class Provider {
        private String hmacSecret = "dev-secret-change-me";

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }
    }

    public static class Outbox {
        private boolean enabled = true;
        private long pollIntervalMs = 2000;
        private String sqsQueueUrl = "";
        private String sqsRegion = "us-east-1";
        private String sqsEndpoint = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public String getSqsQueueUrl() {
            return sqsQueueUrl;
        }

        public void setSqsQueueUrl(String sqsQueueUrl) {
            this.sqsQueueUrl = sqsQueueUrl;
        }

        public String getSqsRegion() {
            return sqsRegion;
        }

        public void setSqsRegion(String sqsRegion) {
            this.sqsRegion = sqsRegion;
        }

        public String getSqsEndpoint() {
            return sqsEndpoint;
        }

        public void setSqsEndpoint(String sqsEndpoint) {
            this.sqsEndpoint = sqsEndpoint;
        }
    }

    public static class Demo {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
