package io.xprevel.util.api.spring.auto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.compatibility")
public class ApiCompatibilityProperties {
    private String versionHeader = "X-API-Version";

    private String defaultVersion;

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getVersionHeader() {
        return versionHeader;
    }

    public void setVersionHeader(String versionHeader) {
        this.versionHeader = versionHeader;
    }
}
