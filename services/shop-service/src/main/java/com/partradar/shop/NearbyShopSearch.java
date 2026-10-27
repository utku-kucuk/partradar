package com.partradar.shop;

import com.partradar.shop.ShopDtos.NearbyShop;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Read side of the shop model: finds shops around a point with PostGIS. Distances are measured on
 * the geography type, so they are metres on the WGS 84 spheroid.
 */
@Component
class NearbyShopSearch {

  static final int MAX_RESULTS = 50;

  private static final String QUERY =
      """
      SELECT id, name, phone, address, categories,
             ST_Y(location) AS latitude,
             ST_X(location) AS longitude,
             ST_Distance(location::geography, origin.point) AS distance_meters
      FROM shop,
           (SELECT ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography AS point) AS origin
      WHERE ST_DWithin(location::geography, origin.point, :radiusMeters)
        AND (CAST(:category AS text) IS NULL OR CAST(:category AS text) = ANY (categories))
      ORDER BY distance_meters
      LIMIT :limit
      """;

  private final JdbcClient jdbc;

  NearbyShopSearch(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  List<NearbyShop> find(double lat, double lon, double radiusKm, PartCategory category) {
    return jdbc.sql(QUERY)
        .param("lat", lat)
        .param("lon", lon)
        .param("radiusMeters", radiusKm * 1000)
        .param("category", category == null ? null : category.name())
        .param("limit", MAX_RESULTS)
        .query(
            (rs, row) ->
                new NearbyShop(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("address"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    categories(rs.getArray("categories")),
                    Math.round(rs.getDouble("distance_meters"))))
        .list();
  }

  private static Set<PartCategory> categories(Array array) throws SQLException {
    Set<PartCategory> result = EnumSet.noneOf(PartCategory.class);
    Arrays.stream((String[]) array.getArray()).map(PartCategory::valueOf).forEach(result::add);
    return result;
  }
}
