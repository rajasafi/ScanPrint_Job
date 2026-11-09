package com.printdoc.api.config;

import com.printdoc.api.plan.Plan;
import com.printdoc.api.plan.PlanRepository;
import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopRepository;
import com.printdoc.api.shop.ShopUser;
import com.printdoc.api.shop.ShopUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {
    @Bean CommandLineRunner seed(PlanRepository plans,ShopRepository shops,ShopUserRepository users,PasswordEncoder encoder,@Value("${printdoc.seed.enabled:true}") boolean enabled,@Value("${printdoc.auth.bootstrap-email:}") String adminEmail,@Value("${printdoc.auth.bootstrap-password:}") String adminPassword,@Value("${printdoc.auth.bootstrap-enabled:false}") boolean bootstrapEnabled){
        return args->{
            if(!enabled)return;
            if(plans.count()==0){plans.save(new Plan("basic","Basic","Single-printer prototype plan",14900,1,150,25));plans.save(new Plan("starter","Starter","Two-printer routing prototype",29900,2,300,35));plans.save(new Plan("pro","Pro","Three-printer workflow prototype",59900,3,500,50));}
            shops.findByShopCodeIgnoreCase("PRINTDOC_DEMO").orElseGet(()->shops.save(new Shop("PRINTDOC_DEMO","PrintDoc sample shop")));
            if(bootstrapEnabled&&!adminEmail.isBlank()&&!adminPassword.isBlank()&&adminPassword.length()>=12){
                String email=adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
                if(!users.existsByEmailIgnoreCase(email)){
                    String demoCode="BOOTSTRAP_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(java.util.Locale.ROOT);
                    Shop shop=shops.save(new Shop(demoCode,"PrintDoc sample shop"));
                    users.save(new ShopUser(shop,email,encoder.encode(adminPassword),"Shop owner",ShopUser.Role.OWNER));
                }
            }
        };
    }
}
