package com.partradar.shop;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Request and response bodies of the shop API. */
final class ShopDtos {

  private ShopDtos() {}

  record RegisterShopRequest(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 30) String phone,
      @NotBlank @Size(max = 300) String address,
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
      @NotEmpty Set<PartCategory> categories) {}

  record ShopResponse(
      UUID id,
      String ownerId,
      String name,
      String phone,
      String address,
      double latitude,
      double longitude,
      Set<PartCategory> categories,
      Instant createdAt) {

    static ShopResponse of(Shop shop) {
      return new ShopResponse(
          shop.id(),
          shop.ownerId(),
          shop.name(),
          shop.phone(),
          shop.address(),
          shop.latitude(),
          shop.longitude(),
          shop.categories(),
          shop.createdAt());
    }
  }

  record NearbyShop(
      UUID id,
      String name,
      String phone,
      String address,
      double latitude,
      double longitude,
      Set<PartCategory> categories,
      long distanceMeters) {}
}
