package com.partradar.shop;

import com.partradar.shop.ShopDtos.NearbyShop;
import com.partradar.shop.ShopDtos.RegisterShopRequest;
import com.partradar.shop.ShopDtos.ShopResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/shops")
@Tag(name = "Shops")
class ShopController {

  private final ShopService shops;

  ShopController(ShopService shops) {
    this.shops = shops;
  }

  @PostMapping
  @PreAuthorize("hasRole('SHOP_OWNER')")
  @Operation(summary = "Register a shop owned by the caller")
  ResponseEntity<ShopResponse> register(
      @AuthenticationPrincipal Jwt caller, @Valid @RequestBody RegisterShopRequest request) {
    Shop shop = shops.register(caller.getSubject(), request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(shop.id())
            .toUri();
    return ResponseEntity.created(location).body(ShopResponse.of(shop));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a shop")
  ShopResponse get(@PathVariable UUID id) {
    return ShopResponse.of(shops.get(id));
  }

  @GetMapping("/nearby")
  @Operation(summary = "Shops within a radius of a point, closest first")
  List<NearbyShop> nearby(
      @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
      @RequestParam @DecimalMin("-180") @DecimalMax("180") double lon,
      @RequestParam(defaultValue = "10") @DecimalMin("0.1") @DecimalMax("50") double radiusKm,
      @RequestParam(required = false) PartCategory category) {
    return shops.nearby(lat, lon, radiusKm, category);
  }
}
