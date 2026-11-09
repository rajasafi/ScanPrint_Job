package com.printdoc.api.shop;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ShopDtos {
    private ShopDtos(){}
    public record PublicShop(String shopCode,String displayName,boolean acceptsOrders,int bwRateMinor,int colorRateMinor,int duplexRateMinor,String currency){}
    public record Profile(String shopCode,String displayName,String email,String role,int bwRateMinor,int colorRateMinor,int duplexRateMinor,Instant createdAt,String phone,String printerBrand,String subscriptionStatus,Instant trialEndsAt){}
    public record UpdateProfile(@NotBlank @Size(min=2,max=100) String displayName,@Min(0) @Max(100000) int bwRateMinor,@Min(0) @Max(100000) int colorRateMinor,@Min(0) @Max(100000) int duplexRateMinor){}
    public record ProfileUpdated(UUID id,String shopCode,String displayName,int bwRateMinor,int colorRateMinor,int duplexRateMinor){}
}
