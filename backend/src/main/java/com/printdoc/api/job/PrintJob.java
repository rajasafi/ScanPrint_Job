package com.printdoc.api.job;

import com.printdoc.api.shop.Shop;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="print_jobs", indexes={@Index(name="idx_job_shop_created",columnList="shop_id,created_at"),@Index(name="idx_job_status",columnList="status")})
public class PrintJob {
    public enum Status { AWAITING_PAYMENT, WAITING_COUNTER_APPROVAL, QUEUED, PRINTING, COMPLETED, CANCELLED, FAILED }
    public enum PaymentMethod { UPI, CASH }
    public enum ColorMode { BLACK_WHITE, COLOR }
    public enum DuplexMode { SIMPLEX, DUPLEX }
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="public_job_id",nullable=false,unique=true,length=22) private String publicJobId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @Column(name="file_name",nullable=false,length=180) private String fileName;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="mime_type",nullable=false,length=100) private String mimeType;
    @Column(name="sha256",length=64) private String sha256;
    @Column(name="total_pages",nullable=false) private int totalPages;
    @Column(name="page_range",nullable=false,length=32) private String pageRange;
    @Column(nullable=false) private int copies;
    @Enumerated(EnumType.STRING) @Column(name="color_mode",nullable=false,length=20) private ColorMode colorMode;
    @Enumerated(EnumType.STRING) @Column(name="duplex_mode",nullable=false,length=20) private DuplexMode duplexMode;
    @Column(name="paper_size",nullable=false,length=20) private String paperSize;
    @Enumerated(EnumType.STRING) @Column(name="payment_method",nullable=false,length=20) private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private Status status;
    @Column(name="amount_minor",nullable=false) private long amountMinor;
    @Column(nullable=false,length=3) private String currency="INR";
    @Column(name="idempotency_key",length=80) private String idempotencyKey;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    @Column(name="purge_after",nullable=false) private Instant purgeAfter;
    protected PrintJob() {}
    public PrintJob(String publicJobId,Shop shop,String fileName,long fileSize,String mimeType,String sha256,int totalPages,String pageRange,int copies,ColorMode colorMode,DuplexMode duplexMode,String paperSize,PaymentMethod paymentMethod,Status status,long amountMinor,String idempotencyKey,Instant purgeAfter){
        this.publicJobId=publicJobId;this.shop=shop;this.fileName=fileName;this.fileSize=fileSize;this.mimeType=mimeType;this.sha256=sha256;this.totalPages=totalPages;this.pageRange=pageRange;this.copies=copies;this.colorMode=colorMode;this.duplexMode=duplexMode;this.paperSize=paperSize;this.paymentMethod=paymentMethod;this.status=status;this.amountMinor=amountMinor;this.idempotencyKey=idempotencyKey;this.purgeAfter=purgeAfter;
    }
    public boolean canTransitionTo(Status next){
        if(next==null)return false;
        if(status==next)return true;
        return switch(status){
            case AWAITING_PAYMENT -> next==Status.CANCELLED;
            case WAITING_COUNTER_APPROVAL -> next==Status.QUEUED||next==Status.CANCELLED;
            case QUEUED -> next==Status.PRINTING||next==Status.CANCELLED;
            case PRINTING -> next==Status.COMPLETED||next==Status.FAILED||next==Status.CANCELLED;
            case COMPLETED,CANCELLED,FAILED -> false;
        };
    }
    public void transition(Status next){if(!canTransitionTo(next))throw new IllegalStateException("Invalid print-job transition from "+status+" to "+next);this.status=next;this.updatedAt=Instant.now();}
    public UUID getId(){return id;} public String getPublicJobId(){return publicJobId;} public Shop getShop(){return shop;} public String getFileName(){return fileName;} public long getFileSize(){return fileSize;} public String getMimeType(){return mimeType;} public String getSha256(){return sha256;} public int getTotalPages(){return totalPages;} public String getPageRange(){return pageRange;} public int getCopies(){return copies;} public ColorMode getColorMode(){return colorMode;} public DuplexMode getDuplexMode(){return duplexMode;} public String getPaperSize(){return paperSize;} public PaymentMethod getPaymentMethod(){return paymentMethod;} public Status getStatus(){return status;} public long getAmountMinor(){return amountMinor;} public String getCurrency(){return currency;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getPurgeAfter(){return purgeAfter;}
}
