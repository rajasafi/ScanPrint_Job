package com.printdoc.api.job;

import com.printdoc.api.shop.Shop;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="uploaded_documents",indexes=@Index(name="idx_upload_shop_expiry",columnList="shop_id,expires_at"))
public class UploadedDocument {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="upload_id",nullable=false,unique=true,length=22) private String uploadId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @Column(name="original_name",nullable=false,length=180) private String originalName;
    @Column(name="storage_name",nullable=false,length=120) private String storageName;
    @Column(name="job_id",length=22) private String publicJobId;
    @Column(name="mime_type",nullable=false,length=100) private String mimeType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="sha256",nullable=false,length=64) private String sha256;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean consumed=false;
    protected UploadedDocument(){}
    public UploadedDocument(String uploadId,Shop shop,String originalName,String storageName,String mimeType,long fileSize,String sha256,Instant expiresAt){this.uploadId=uploadId;this.shop=shop;this.originalName=originalName;this.storageName=storageName;this.mimeType=mimeType;this.fileSize=fileSize;this.sha256=sha256;this.expiresAt=expiresAt;}
    public void markConsumed(String jobId){this.consumed=true;this.publicJobId=jobId;}
    public UUID getId(){return id;} public String getUploadId(){return uploadId;} public Shop getShop(){return shop;} public String getOriginalName(){return originalName;} public String getStorageName(){return storageName;} public String getMimeType(){return mimeType;} public long getFileSize(){return fileSize;} public String getSha256(){return sha256;} public Instant getCreatedAt(){return createdAt;} public Instant getExpiresAt(){return expiresAt;} public boolean isConsumed(){return consumed;} public String getPublicJobId(){return publicJobId;}
}
