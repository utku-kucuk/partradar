package com.partradar.platform.testapp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Decodes the bearer value as a realm role, e.g. {@code Bearer SHOP_OWNER}. */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtDecoderConfiguration {

  @Bean
  JwtDecoder jwtDecoder() {
    return token ->
        Jwt.withTokenValue(token)
            .header("alg", "none")
            .subject("user-" + token.toLowerCase())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("realm_access", Map.of("roles", List.of(token)))
            .build();
  }
}
