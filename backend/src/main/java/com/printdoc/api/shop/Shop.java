package com.printdoc.api.shop;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="shops", indexes=@Index(name="idx_shop_code",columnList="shop_code",unique=true))
public class Shop {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="shop_code",nullable=false,unique=true,length=32) private String shopCode;
    @Column(nullable=false,length=100) private String displayName;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false) private int bwRate=200;
    @Column(nullable=false) private int colorRate=500;
    @Column(nullable=false) private int duplexRate=300;
    @Column(length=32) private String phone;
    @Column(name="printer_brand",length=100) private String printerBrand;
    @Column(name="subscription_status",nullable=false,length=24) private String subscriptionStatus="TRIAL";
    @Column(name="trial_started_at") private Instant trialStartedAt;
    @Column(name="trial_ends_at") private Instant trialEndsAt;
    @Column(name="agent_secret_hash",length=100) private String agentSecretHash;
    @Column(name="agent_secret_created_at") private Instant agentSecretCreatedAt;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected Shop() {}
    public Shop(String shopCode,String displayName){this.shopCode=shopCode;this.displayName=displayName;}
    public UUID getId(){return id;} public String getShopCode(){return shopCode;} public String getDisplayName(){return displayName;} public boolean isActive(){return active;}
    public int getBwRate(){return bwRate;} public int getColorRate(){return colorRate;} public int getDuplexRate(){return duplexRate;} public Instant getCreatedAt(){return createdAt;}
    public String getPhone(){return phone;} public String getPrinterBrand(){return printerBrand;}
    public String getSubscriptionStatus(){return subscriptionStatus;} public Instant getTrialStartedAt(){return trialStartedAt;} public Instant getTrialEndsAt(){return trialEndsAt;}
    public String getAgentSecretHash(){return agentSecretHash;} public Instant getAgentSecretCreatedAt(){return agentSecretCreatedAt;}
    public void configureRegistration(String phone,String printerBrand,Instant trialStartedAt,Instant trialEndsAt){
        this.phone=phone==null||phone.isBlank()?null:phone.trim();
        this.printerBrand=printerBrand==null||printerBrand.isBlank()?null:printerBrand.trim();
        this.subscriptionStatus="TRIAL"; this.trialStartedAt=trialStartedAt; this.trialEndsAt=trialEndsAt;
    }
    public void replaceAgentSecret(String hash,Instant createdAt){this.agentSecretHash=hash;this.agentSecretCreatedAt=createdAt;}
    public void updateProfile(String displayName,int bwRate,int colorRate,int duplexRate){this.displayName=displayName;this.bwRate=bwRate;this.colorRate=colorRate;this.duplexRate=duplexRate;}
}
