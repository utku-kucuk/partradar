package com.partradar.shop;

import com.partradar.platform.web.NotFoundException;
import com.partradar.shop.ShopDtos.NearbyShop;
import com.partradar.shop.ShopDtos.RegisterShopRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ShopService {

  private final ShopRepository shops;
  private final NearbyShopSearch nearbySearch;

  ShopService(ShopRepository shops, NearbyShopSearch nearbySearch) {
    this.shops = shops;
    this.nearbySearch = nearbySearch;
  }

  @Transactional
  Shop register(String ownerId, RegisterShopRequest request) {
    return shops.save(
        new Shop(
            ownerId,
            request.name().strip(),
            request.phone().strip(),
            request.address().strip(),
            request.latitude(),
            request.longitude(),
            request.categories()));
  }

  @Transactional(readOnly = true)
  Shop get(UUID id) {
    return shops.findById(id).orElseThrow(() -> new NotFoundException("Shop", id));
  }

  @Transactional(readOnly = true)
  List<NearbyShop> nearby(double lat, double lon, double radiusKm, PartCategory category) {
    return nearbySearch.find(lat, lon, radiusKm, category);
  }
}
