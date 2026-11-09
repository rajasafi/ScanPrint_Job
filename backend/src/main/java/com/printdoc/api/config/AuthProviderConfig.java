package com.printdoc.api.config;

import com.printdoc.api.shop.ShopUser;
import com.printdoc.api.shop.ShopUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthProviderConfig {
    @Bean UserDetailsService printDocUserDetailsService(ShopUserRepository users){
        return email->users.findByEmailIgnoreCase(email).filter(ShopUser::isActive)
            .map(u->User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRole().name()).build())
            .orElseThrow(()->new UsernameNotFoundException("Account not found."));
    }
    @Bean AuthenticationProvider printDocAuthenticationProvider(UserDetailsService details,PasswordEncoder encoder){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider(details);provider.setPasswordEncoder(encoder);return provider;
    }
}
