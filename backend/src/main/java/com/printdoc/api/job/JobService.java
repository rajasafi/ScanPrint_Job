package com.printdoc.api.job;

import com.printdoc.api.common.ApiException;
import com.printdoc.api.common.PageResponse;
import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class JobService {
    private static final List<String> ALLOWED=List.of("application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","image/jpeg","image/png","image/webp","image/avif","image/heic","image/heif","image/tiff","image/bmp");
    private static final Pattern RANGE=Pattern.compile("(?i)all|[1-9][0-9]{0,3}(?:-[1-9][0-9]{0,3})?");
    private final ShopRepository shops; private final UploadedDocumentRepository uploads; private final PrintJobRepository jobs; private final JobEventRepository events; private final DocumentStorageService storage; private final long maxBytes; private final java.security.SecureRandom secureRandom=new java.security.SecureRandom();
    public JobService(ShopRepository shops,UploadedDocumentRepository uploads,PrintJobRepository jobs,JobEventRepository events,DocumentStorageService storage,@Value("${printdoc.upload.max-file-size-mb:20}") int maxMb){this.shops=shops;this.uploads=uploads;this.jobs=jobs;this.events=events;this.storage=storage;this.maxBytes=Math.max(1,maxMb)*1024L*1024L;}
    @Transactional
    public JobDtos.UploadView upload(String shopCode,MultipartFile file){Shop shop=shops.findByShopCodeIgnoreCase(shopCode.trim()).filter(Shop::isActive).orElseThrow(()->ApiException.notFound("Shop was not found."));if(file==null||file.isEmpty())throw new ApiException(HttpStatus.BAD_REQUEST,"Choose a non-empty file.");if(file.getSize()>maxBytes)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"File exceeds the configured upload size limit.");String mime=file.getContentType()==null?"application/octet-stream":file.getContentType().toLowerCase(Locale.ROOT);if(!ALLOWED.contains(mime))throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"This file type is not supported.");String name=safeName(file.getOriginalFilename());try{String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));String storageName=storage.store(file);Instant expiry=Instant.now().plus(2,ChronoUnit.HOURS);UploadedDocument doc=uploads.save(new UploadedDocument(publicJobId(),shop,name,storageName,mime,file.getSize(),hash,expiry));return new JobDtos.UploadView(doc.getUploadId(),doc.getOriginalName(),doc.getMimeType(),doc.getFileSize(),doc.getSha256(),doc.getExpiresAt());}catch(Exception e){throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not accept this upload.");}}
    @Transactional
    public JobDtos.JobView create(String shopCode,JobDtos.CreateRequest request){Shop shop=shops.findByShopCodeIgnoreCase(shopCode.trim()).filter(Shop::isActive).orElseThrow(()->ApiException.notFound("Shop was not found."));return createForShop(shop,request);}
    @Transactional
    public JobDtos.JobView createForShop(Shop shop,JobDtos.CreateRequest request){var existing=jobs.findByIdempotencyKeyAndShopId(request.idempotencyKey(),shop.getId());if(existing.isPresent())return view(existing.get());List<UploadedDocument> docs=request.uploadIds().stream().map(id->uploads.findByUploadIdAndShopId(id,shop.getId()).filter(d->!d.isConsumed()&&d.getExpiresAt().isAfter(Instant.now())).orElseThrow(()->ApiException.notFound("An upload is missing or expired."))).toList();if(docs.stream().distinct().count()!=docs.size())throw new ApiException(HttpStatus.BAD_REQUEST,"Do not include an upload more than once.");int pages=docs.stream().mapToInt(d->estimatedPages(d.getFileSize(),d.getMimeType())).sum();String range=request.pageRange().trim();if(!RANGE.matcher(range).matches())throw new ApiException(HttpStatus.BAD_REQUEST,"Page range must be all, a page number, or a range such as 1-3.");int selected=selectedPages(range,pages);long rate=request.colorMode()==PrintJob.ColorMode.COLOR?shop.getColorRate():(request.duplexMode()==PrintJob.DuplexMode.DUPLEX?shop.getDuplexRate():shop.getBwRate());long amount=Math.multiplyExact(Math.multiplyExact((long)selected,request.copies()),rate);PrintJob.Status initial=request.paymentMethod()==PrintJob.PaymentMethod.UPI?PrintJob.Status.AWAITING_PAYMENT:PrintJob.Status.WAITING_COUNTER_APPROVAL;UploadedDocument first=docs.get(0);PrintJob job=jobs.save(new PrintJob(publicJobId(),shop,first.getOriginalName(),docs.stream().mapToLong(UploadedDocument::getFileSize).sum(),first.getMimeType(),first.getSha256(),pages,range,request.copies(),request.colorMode(),request.duplexMode(),request.paperSize(),request.paymentMethod(),initial,amount,request.idempotencyKey(),Instant.now().plus(2,ChronoUnit.HOURS)));docs.forEach(doc->doc.markConsumed(job.getPublicJobId()));events.save(new JobEvent(job,"CREATED","CUSTOMER","Quote generated; payment and printing are not integrated."));return view(job);}
    @Transactional
    public JobDtos.JobView transitionFromAgent(String jobId,UUID shopId,PrintJob.Status next,String note){
        PrintJob job=jobs.lockByPublicIdAndShop(jobId,shopId).orElseThrow(()->ApiException.notFound("Job was not found."));
        if(job.getStatus()==next)return view(job);
        if(job.getStatus()!=PrintJob.Status.PRINTING)throw ApiException.conflict("An agent result is only accepted for a printing job.");
        if(next!=PrintJob.Status.COMPLETED&&next!=PrintJob.Status.FAILED)throw ApiException.conflict("An agent may only report completed or failed jobs.");
        job.transition(next);events.save(new JobEvent(job,next.name(),"DESKTOP_AGENT",note));return view(job);
    }

    @Transactional
    public JobDtos.JobView startPrinting(String jobId,String shopCode){
        Shop shop=shops.findByShopCodeIgnoreCase(shopCode).filter(Shop::isActive).orElseThrow(()->ApiException.notFound("Shop was not found."));
        PrintJob job=jobs.lockByPublicIdAndShop(jobId,shop.getId()).orElseThrow(()->ApiException.notFound("Job was not found."));
        if(job.getStatus()==PrintJob.Status.PRINTING)return view(job);
        if(job.getStatus()!=PrintJob.Status.QUEUED)throw ApiException.conflict("Only queued jobs can be dispatched to an agent.");
        job.transition(PrintJob.Status.PRINTING);events.save(new JobEvent(job,"PRINTING","PRINT_SERVER","Job dispatched to an authenticated agent."));return view(job);
    }

    @Transactional(readOnly=true)
    public PageResponse<JobDtos.JobView> list(UUID shopId,PrintJob.Status status,int page,int size){var pageable=PageRequest.of(Math.max(0,page),Math.min(Math.max(size,1),100));var result=status==null?jobs.findByShopIdOrderByCreatedAtDesc(shopId,pageable):jobs.findByShopIdAndStatusOrderByCreatedAtDesc(shopId,status,pageable);return PageResponse.from(result.map(this::view));}
    @Transactional(readOnly=true)
    public JobDtos.JobView getForOwner(String jobId,UUID shopId){return view(jobs.findByPublicJobIdAndShopId(jobId,shopId).orElseThrow(()->ApiException.notFound("Job was not found.")));}
    @Transactional
    public JobDtos.JobView transitionOwner(String jobId,UUID shopId,JobDtos.StatusUpdate update){PrintJob job=jobs.lockByPublicIdAndShop(jobId,shopId).orElseThrow(()->ApiException.notFound("Job was not found."));if(job.getStatus()==PrintJob.Status.COMPLETED||job.getStatus()==PrintJob.Status.CANCELLED||job.getStatus()==PrintJob.Status.FAILED)throw ApiException.conflict("This job is already in a final state.");if(job.getPaymentMethod()==PrintJob.PaymentMethod.UPI&&update.status()!=PrintJob.Status.CANCELLED&&update.status()!=PrintJob.Status.AWAITING_PAYMENT)throw new ApiException(HttpStatus.CONFLICT,"Online payments are not connected; UPI jobs cannot be approved or queued in this API demo.");if(job.getPaymentMethod()==PrintJob.PaymentMethod.CASH&&job.getStatus()==PrintJob.Status.WAITING_COUNTER_APPROVAL&&update.status()!=PrintJob.Status.QUEUED&&update.status()!=PrintJob.Status.CANCELLED)throw new ApiException(HttpStatus.CONFLICT,"Cash jobs may only be approved to the sample queue or cancelled.");if(update.status()!=PrintJob.Status.CANCELLED&&job.getPaymentMethod()==PrintJob.PaymentMethod.CASH&&job.getStatus()!=PrintJob.Status.WAITING_COUNTER_APPROVAL)throw new ApiException(HttpStatus.CONFLICT,"Only counter-approval jobs can be approved in the demo.");job.transition(update.status());events.save(new JobEvent(job,update.status().name(),"SHOP_OWNER",update.note()));return view(job);}
    @Transactional(readOnly=true)
    public JobDtos.JobView publicStatus(String jobId){UploadedDocument doc=uploads.findByPublicJobId(jobId).stream().findFirst().orElseThrow(()->ApiException.notFound("Job was not found."));PrintJob job=jobs.findByPublicJobIdAndShopId(jobId,doc.getShop().getId()).orElseThrow(()->ApiException.notFound("Job was not found."));if(job.getPurgeAfter().isBefore(Instant.now()))throw ApiException.notFound("Job was not found.");return view(job);}
    @Transactional
    public int purgeExpired(){Instant now=Instant.now();List<UploadedDocument> expired=uploads.findByExpiresAtBefore(now);int count=0;for(UploadedDocument doc:expired){try{storage.delete(doc.getStorageName());uploads.delete(doc);count++;}catch(IOException e){/* Leave the row for a future retry. */}}return count;}
    public JobDtos.JobView view(PrintJob j){return new JobDtos.JobView(j.getPublicJobId(),j.getFileName(),j.getMimeType(),j.getFileSize(),j.getTotalPages(),j.getPageRange(),j.getCopies(),j.getColorMode().name(),j.getDuplexMode().name(),j.getPaperSize(),j.getPaymentMethod().name(),j.getStatus().name(),j.getAmountMinor(),j.getCurrency(),j.getCreatedAt(),j.getUpdatedAt());}
    private String publicJobId(){byte[] bytes=new byte[20];secureRandom.nextBytes(bytes);return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static int estimatedPages(long bytes,String mime){return mime.startsWith("image/")?1:(int)Math.max(1,Math.min(1000,Math.ceil(bytes/90_000.0)));}
    private static int selectedPages(String range,int pages){if(range.equalsIgnoreCase("all"))return pages;String[] parts=range.split("-");int start=Integer.parseInt(parts[0]);int end=parts.length==1?start:Integer.parseInt(parts[1]);if(start<1||end<start||end>pages)throw new ApiException(HttpStatus.BAD_REQUEST,"Page range is outside the estimated document page count.");return end-start+1;}
    private static String safeName(String input){String name=input==null?"document":input.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[^A-Za-z0-9._ -]","_").strip();if(name.isBlank())name="document";return name.length()>180?name.substring(0,180):name;}
}
