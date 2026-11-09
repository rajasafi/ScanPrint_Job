package com.printdoc.api.shop;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="shop_users", indexes=@Index(name="idx_user_email",columnList="email",unique=true))
public class ShopUser {
    public enum Role { OWNER, ADMIN }
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id") private Shop shop;
    @Column(nullable=false,unique=true,length=254) private String email;
    @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
    @Column(nullable=false,length=100) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Role role=Role.OWNER;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected ShopUser() {}
    public ShopUser(Shop shop,String email,String passwordHash,String displayName,Role role){this.shop=shop;this.email=email;this.passwordHash=passwordHash;this.displayName=displayName;this.role=role;}
    public UUID getId(){return id;} public Shop getShop(){return shop;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getDisplayName(){return displayName;} public Role getRole(){return role;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;}
}
