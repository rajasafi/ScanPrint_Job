package com.printdoc.api.shop;

import com.printdoc.api.common.ApiResponse;
import com.printdoc.api.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShopController {
    private final ShopService service; private final CurrentUser current;
    public ShopController(ShopService service,CurrentUser current){this.service=service;this.current=current;}
    @GetMapping("/api/v1/public/shops/{shopCode}") public ApiResponse<ShopDtos.PublicShop> publicShop(@PathVariable String shopCode){return ApiResponse.ok("Public shop settings.",service.publicByCode(shopCode));}
    @GetMapping("/api/v1/owner/profile") @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')") public ApiResponse<ShopDtos.Profile> profile(HttpServletRequest req){return ApiResponse.ok("Shop profile.",service.profile(current.require(req)));}
    @PutMapping("/api/v1/owner/profile") @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')") public ApiResponse<ShopDtos.ProfileUpdated> update(HttpServletRequest req,@Valid @RequestBody ShopDtos.UpdateProfile body){return ApiResponse.ok("Shop settings updated.",service.update(current.require(req),body));}
}
