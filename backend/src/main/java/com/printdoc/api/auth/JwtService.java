package com.printdoc.api.auth;

import com.printdoc.api.shop.ShopUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;
    public JwtService(@Value("${printdoc.jwt.secret:}") String secret,@Value("${printdoc.jwt.ttl:PT8H}") Duration ttl) {
        if(secret==null||secret.length()<32) throw new IllegalStateException("JWT_SECRET must be set to a random value of at least 32 characters before starting the API.");
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.ttl=ttl;
    }
    public String issue(ShopUser user){if(user.getId()==null)throw new IllegalStateException("User must be persisted before issuing tokens.");Instant now=Instant.now();return Jwts.builder().subject(user.getId().toString()).claim("role",user.getRole().name()).claim("shopId",user.getShop()==null||user.getShop().getId()==null?null:user.getShop().getId().toString()).issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl))).signWith(key).compact();}
    public Claims parse(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();}
    public Duration getTtl(){return ttl;}
    public UUID getUserId(Claims claims){return UUID.fromString(claims.getSubject());}
}
