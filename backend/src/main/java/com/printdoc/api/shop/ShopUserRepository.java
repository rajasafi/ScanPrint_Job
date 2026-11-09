package com.printdoc.api.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ShopUserRepository extends JpaRepository<ShopUser,UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="shop")
    Optional<ShopUser> findByEmailIgnoreCase(String email);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="shop")
    Optional<ShopUser> findWithShopById(UUID id);
    boolean existsByEmailIgnoreCase(String email);
}
