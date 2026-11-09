package com.printdoc.api.shop;

import com.printdoc.api.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class ShopService {
    private final ShopRepository shops;
    public ShopService(ShopRepository shops){this.shops=shops;}
    @Transactional(readOnly=true) public ShopDtos.PublicShop publicByCode(String code){Shop s=shops.findByShopCodeIgnoreCase(code.trim()).filter(Shop::isActive).orElseThrow(()->ApiException.notFound("Shop was not found."));return new ShopDtos.PublicShop(s.getShopCode(),s.getDisplayName(),true,s.getBwRate(),s.getColorRate(),s.getDuplexRate(),"INR");}
    @Transactional(readOnly=true) public ShopDtos.Profile profile(ShopUser user){Shop s=user.getShop();return new ShopDtos.Profile(s.getShopCode(),s.getDisplayName(),user.getEmail(),user.getRole().name(),s.getBwRate(),s.getColorRate(),s.getDuplexRate(),s.getCreatedAt(),s.getPhone(),s.getPrinterBrand(),s.getSubscriptionStatus(),s.getTrialEndsAt());}
    @Transactional public ShopDtos.ProfileUpdated update(ShopUser user,ShopDtos.UpdateProfile update){Shop s=user.getShop();s.updateProfile(update.displayName().trim(),update.bwRateMinor(),update.colorRateMinor(),update.duplexRateMinor());return new ShopDtos.ProfileUpdated(s.getId(),s.getShopCode(),s.getDisplayName(),s.getBwRate(),s.getColorRate(),s.getDuplexRate());}
}
