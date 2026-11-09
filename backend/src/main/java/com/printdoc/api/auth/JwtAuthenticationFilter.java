package com.printdoc.api.auth;

import com.printdoc.api.shop.ShopUser;
import com.printdoc.api.shop.ShopUserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final ShopUserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService,ShopUserRepository users){this.jwtService=jwtService;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")&&SecurityContextHolder.getContext().getAuthentication()==null){
            try { Claims claims=jwtService.parse(header.substring(7)); UUID userId=jwtService.getUserId(claims); ShopUser user=users.findWithShopById(userId).filter(ShopUser::isActive).orElse(null);
                if(user!=null){var auth=new UsernamePasswordAuthenticationToken(user.getId().toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name())));SecurityContextHolder.getContext().setAuthentication(auth);request.setAttribute("authenticatedUserId",user.getId());}
            } catch(Exception ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request,response);
    }
}
