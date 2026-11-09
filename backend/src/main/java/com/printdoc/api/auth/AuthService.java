package com.printdoc.api.auth;

import com.printdoc.api.common.ApiException;
import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopRepository;
import com.printdoc.api.shop.ShopUser;
import com.printdoc.api.shop.ShopUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL=Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");
    private static final Pattern SHOP_CODE=Pattern.compile("^[A-Z0-9_-]{4,32}$");
    private final ShopUserRepository users;
    private final ShopRepository shops;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final boolean registrationEnabled;
    private final SecureRandom random=new SecureRandom();

    public AuthService(ShopUserRepository users,ShopRepository shops,PasswordEncoder passwords,JwtService jwt,
                       @Value("${printdoc.auth.registration-enabled:false}") boolean registrationEnabled){
        this.users=users;this.shops=shops;this.passwords=passwords;this.jwt=jwt;this.registrationEnabled=registrationEnabled;
    }

    @Transactional(readOnly=true)
    public AuthDtos.LoginView login(AuthDtos.LoginRequest request){
        String email=request.email().trim();
        ShopUser user=users.findByEmailIgnoreCase(email).orElseThrow(()->ApiException.unauthorized("Email or password is incorrect."));
        if(!user.isActive()||!passwords.matches(request.password(),user.getPasswordHash()))throw ApiException.unauthorized("Email or password is incorrect.");
        return session(user);
    }

    @Transactional
    public AuthDtos.LoginView register(AuthDtos.RegisterRequest request){
        if(!registrationEnabled)throw new ApiException(HttpStatus.FORBIDDEN,"Public registration is disabled on this server. Start the local dev API with ALLOW_PUBLIC_REGISTRATION=true.");
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        if(!EMAIL.matcher(email).matches())throw new ApiException(HttpStatus.BAD_REQUEST,"Enter a valid email address.");
        if(users.existsByEmailIgnoreCase(email))throw ApiException.conflict("An account already exists for this email. Try signing in instead.");
        String code=resolveShopCode(request.shopCode(),request.shopName());
        Shop shop=new Shop(code,request.shopName().trim());
        Instant trialStarted=Instant.now();
        shop.configureRegistration(request.phone(),request.printerBrand(),trialStarted,trialStarted.plus(48, ChronoUnit.HOURS));
        shops.save(shop);
        ShopUser user=users.save(new ShopUser(shop,email,passwords.encode(request.password()),request.ownerName().trim(),ShopUser.Role.OWNER));
        return session(user);
    }

    public AuthDtos.UserView view(ShopUser user){return new AuthDtos.UserView(user.getId(),user.getEmail(),user.getDisplayName(),user.getRole().name(),user.getShop()==null?null:user.getShop().getShopCode(),user.getShop()==null?null:user.getShop().getDisplayName());}

    private AuthDtos.LoginView session(ShopUser user){return new AuthDtos.LoginView(jwt.issue(user),"Bearer",jwt.getTtl().toSeconds(),view(user));}

    private String resolveShopCode(String requested,String shopName){
        if(requested!=null&&!requested.isBlank()){
            String code=requested.trim().toUpperCase(Locale.ROOT);
            if(!SHOP_CODE.matcher(code).matches())throw new ApiException(HttpStatus.BAD_REQUEST,"Shop code may contain only letters, numbers, underscores, and hyphens (4-32 characters).");
            if(shops.existsByShopCodeIgnoreCase(code))throw ApiException.conflict("That shop code is already in use.");
            return code;
        }
        String base=shopName.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");
        if(base.length()>12)base=base.substring(0,12);
        if(base.length()<4)base=(base+"SHOP").substring(0,4);
        for(int attempt=0;attempt<20;attempt++){
            String candidate=base+"_"+String.format("%04d",random.nextInt(10_000));
            if(!shops.existsByShopCodeIgnoreCase(candidate))return candidate;
        }
        throw new ApiException(HttpStatus.CONFLICT,"Could not allocate a shop code automatically. Please provide one.");
    }
}
