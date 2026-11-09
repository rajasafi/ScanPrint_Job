package com.printdoc.api.common;

import com.printdoc.api.shop.ShopUser;
import com.printdoc.api.shop.ShopUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CurrentUser {
    private final ShopUserRepository users;
    public CurrentUser(ShopUserRepository users){this.users=users;}
    public ShopUser require(HttpServletRequest request){Object value=request.getAttribute("authenticatedUserId");if(!(value instanceof UUID id))throw ApiException.unauthorized("A valid login is required.");return users.findWithShopById(id).filter(ShopUser::isActive).orElseThrow(()->ApiException.unauthorized("Account is inactive or missing."));}
    public UUID requireShopId(HttpServletRequest request){ShopUser user=require(request);if(user.getShop()==null)throw ApiException.unauthorized("This account is not linked to a shop.");return user.getShop().getId();}
}
