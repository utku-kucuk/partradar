package com.partradar.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTests {

  private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

  @Test
  void mapsRealmRolesToRoleAuthorities() {
    Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("SHOP_OWNER", "CUSTOMER"))));

    assertThat(converter.convert(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_SHOP_OWNER", "ROLE_CUSTOMER");
  }

  @Test
  void tokenWithoutRealmAccessHasNoAuthorities() {
    assertThat(converter.convert(token(Map.of("scope", "openid")))).isEmpty();
  }

  private static Jwt token(Map<String, Object> claims) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user-1")
        .issuedAt(Instant.now())
        .claims(c -> c.putAll(claims))
        .build();
  }
}
