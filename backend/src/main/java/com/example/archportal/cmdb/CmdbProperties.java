package com.example.archportal.cmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.cmdb")
public record CmdbProperties(boolean enabled, String baseUrl) {
}
