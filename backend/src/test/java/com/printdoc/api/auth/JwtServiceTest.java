package com.printdoc.api.auth;

import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopUser;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test void issuesAndReadsSignedToken() throws Exception {
        JwtService service=new JwtService("test-secret-value-that-is-very-long-and-not-production",java.time.Duration.ofHours(1));
        Shop shop=new Shop("TEST_SHOP","Sample Shop");
        java.lang.reflect.Field shopId=Shop.class.getDeclaredField("id");shopId.setAccessible(true);shopId.set(shop,java.util.UUID.randomUUID());
        ShopUser user=new ShopUser(shop,"owner@example.invalid","$2a$12$test","Sample owner",ShopUser.Role.OWNER);
        java.lang.reflect.Field userId=ShopUser.class.getDeclaredField("id");userId.setAccessible(true);userId.set(user,java.util.UUID.randomUUID());
        String token=service.issue(user);
        var claims=service.parse(token);
        assertThat(claims.get("role",String.class)).isEqualTo("OWNER");
        assertThat(service.getTtl().toSeconds()).isEqualTo(3600);
    }
}
