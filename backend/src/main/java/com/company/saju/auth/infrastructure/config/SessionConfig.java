package com.company.saju.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 60 * 60 * 24 * 14)  // 14 days
public class SessionConfig {
}
