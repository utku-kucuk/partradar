package com.partradar.shop;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.partradar.platform.security.KeycloakRealmRoleConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ShopApiTests {

  // Search origin in Kadıköy, Istanbul.
  static final double ORIGIN_LAT = 40.9900;
  static final double ORIGIN_LON = 29.0280;

  @Autowired MockMvc mvc;
  @Autowired JdbcClient jdbc;

  @BeforeEach
  void cleanUp() {
    jdbc.sql("DELETE FROM shop").update();
  }

  @Test
  void shopOwnerRegistersShop() throws Exception {
    mvc.perform(
            post("/api/shops")
                .with(user("owner-1", "SHOP_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(shopJson("Moda Oto Yedek Parça", 40.9870, 29.0260, "BRAKES", "ENGINE")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(".*/api/shops/[0-9a-f-]{36}")))
        .andExpect(jsonPath("$.name").value("Moda Oto Yedek Parça"))
        .andExpect(jsonPath("$.ownerId").value("owner-1"))
        .andExpect(jsonPath("$.latitude").value(40.9870))
        .andExpect(jsonPath("$.longitude").value(29.0260))
        .andExpect(jsonPath("$.categories", containsInAnyOrder("BRAKES", "ENGINE")));
  }

  @Test
  void locationHeaderUsesTheHostSeenByTheClient() throws Exception {
    mvc.perform(
            post("/api/shops")
                .with(user("owner-1", "SHOP_OWNER"))
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "api.partradar.example")
                .header("X-Forwarded-Port", "443")
                .contentType(MediaType.APPLICATION_JSON)
                .content(shopJson("Moda Oto Yedek Parça", 40.9870, 29.0260, "BRAKES")))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location", matchesPattern("https://api\\.partradar\\.example/api/shops/.+")));
  }

  @Test
  void customerCannotRegisterShop() throws Exception {
    mvc.perform(
            post("/api/shops")
                .with(user("customer-1", "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(shopJson("Not A Shop", 40.98, 29.02, "BRAKES")))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidShopIsRejectedWithFieldErrors() throws Exception {
    mvc.perform(
            post("/api/shops")
                .with(user("owner-1", "SHOP_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "", "phone": "0212", "address": "Moda", "latitude": 91,
                     "longitude": 29.0, "categories": []}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.latitude").exists())
        .andExpect(jsonPath("$.errors.categories").exists());
  }

  @Test
  void registeredShopCanBeFetched() throws Exception {
    String id = register("Moda Oto Yedek Parça", 40.9870, 29.0260, "BRAKES");

    mvc.perform(get("/api/shops/{id}", id).with(user("customer-1", "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Moda Oto Yedek Parça"));
  }

  @Test
  void unknownShopIsNotFound() throws Exception {
    mvc.perform(
            get("/api/shops/{id}", "00000000-0000-0000-0000-000000000000")
                .with(user("customer-1", "CUSTOMER")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("https://partradar.dev/problems/not-found"));
  }

  @Test
  void nearbyReturnsShopsWithinRadiusClosestFirst() throws Exception {
    register("Kadıköy Fren Merkezi", 40.9800, 29.0450, "BRAKES"); // ~1.8 km
    register("Moda Oto Yedek Parça", 40.9870, 29.0260, "BRAKES", "ENGINE"); // ~0.4 km
    register("Gebze Sanayi Parça", 40.8000, 29.4300, "BRAKES"); // ~39 km

    mvc.perform(nearby(5).with(user("customer-1", "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("Moda Oto Yedek Parça"))
        .andExpect(jsonPath("$[0].distanceMeters").value(closeTo(375.0, 50.0), Double.class))
        .andExpect(jsonPath("$[1].name").value("Kadıköy Fren Merkezi"))
        .andExpect(jsonPath("$[1].distanceMeters").value(closeTo(1750.0, 150.0), Double.class));
  }

  @Test
  void nearbyFiltersByPartCategory() throws Exception {
    register("Moda Oto Yedek Parça", 40.9870, 29.0260, "ENGINE");
    register("Kadıköy Fren Merkezi", 40.9800, 29.0450, "BRAKES");

    mvc.perform(nearby(5).param("category", "BRAKES").with(user("customer-1", "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("Kadıköy Fren Merkezi"));
  }

  @Test
  void nearbyRejectsInvalidCoordinates() throws Exception {
    mvc.perform(
            get("/api/shops/nearby")
                .param("lat", "123")
                .param("lon", "29.0")
                .with(user("customer-1", "CUSTOMER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nearbyRequiresAuthentication() throws Exception {
    mvc.perform(nearby(5)).andExpect(status().isUnauthorized());
  }

  private String register(String name, double lat, double lon, String... categories)
      throws Exception {
    String body =
        mvc.perform(
                post("/api/shops")
                    .with(user("owner-1", "SHOP_OWNER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(shopJson(name, lat, lon, categories)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nearby(
      double radiusKm) {
    return get("/api/shops/nearby")
        .param("lat", String.valueOf(ORIGIN_LAT))
        .param("lon", String.valueOf(ORIGIN_LON))
        .param("radiusKm", String.valueOf(radiusKm));
  }

  private static String shopJson(String name, double lat, double lon, String... categories) {
    String cats =
        String.join(", ", List.of(categories).stream().map(c -> "\"" + c + "\"").toList());
    return """
        {"name": "%s", "phone": "+90 216 555 01 01", "address": "Kadıköy, İstanbul",
         "latitude": %s, "longitude": %s, "categories": [%s]}
        """
        .formatted(name, lat, lon, cats);
  }

  private static JwtRequestPostProcessor user(String subject, String role) {
    return jwt()
        .jwt(j -> j.subject(subject).claim("realm_access", Map.of("roles", List.of(role))))
        .authorities(new KeycloakRealmRoleConverter());
  }
}
