package com.partradar.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Table(name = "shop")
public class Shop {

  private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

  @Id private UUID id;

  @Column(nullable = false)
  private String ownerId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
  private Point location;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(nullable = false, columnDefinition = "text[]")
  private List<String> categories;

  @Column(nullable = false)
  private Instant createdAt;

  protected Shop() {}

  public Shop(
      String ownerId,
      String name,
      String phone,
      String address,
      double latitude,
      double longitude,
      Set<PartCategory> categories) {
    this.id = UUID.randomUUID();
    this.ownerId = ownerId;
    this.name = name;
    this.phone = phone;
    this.address = address;
    this.location = WGS84.createPoint(new Coordinate(longitude, latitude));
    this.categories = categories.stream().map(Enum::name).sorted().toList();
    this.createdAt = Instant.now();
  }

  public UUID id() {
    return id;
  }

  public String ownerId() {
    return ownerId;
  }

  public String name() {
    return name;
  }

  public String phone() {
    return phone;
  }

  public String address() {
    return address;
  }

  public double latitude() {
    return location.getY();
  }

  public double longitude() {
    return location.getX();
  }

  public Set<PartCategory> categories() {
    return categories.stream()
        .map(PartCategory::valueOf)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(PartCategory.class)));
  }

  public Instant createdAt() {
    return createdAt;
  }
}
