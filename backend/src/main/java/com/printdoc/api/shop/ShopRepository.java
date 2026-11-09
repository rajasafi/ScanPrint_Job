package com.printdoc.api.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop,UUID> {
    Optional<Shop> findByShopCodeIgnoreCase(String shopCode);
    boolean existsByShopCodeIgnoreCase(String shopCode);
}
