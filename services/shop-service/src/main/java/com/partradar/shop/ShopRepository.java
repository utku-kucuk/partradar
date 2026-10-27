package com.partradar.shop;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShopRepository extends JpaRepository<Shop, UUID> {}
