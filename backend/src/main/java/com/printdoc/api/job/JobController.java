package com.printdoc.api.job;

import com.printdoc.api.common.ApiResponse;
import com.printdoc.api.common.CurrentUser;
import com.printdoc.api.common.PageResponse;
import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopRepository;
import com.printdoc.api.socket.AgentSocketPublisher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
public class JobController {
    private final JobService jobs; private final CurrentUser current; private final ShopRepository shops; private final AgentSocketPublisher socket;
    public JobController(JobService jobs,CurrentUser current,ShopRepository shops,AgentSocketPublisher socket){this.jobs=jobs;this.current=current;this.shops=shops;this.socket=socket;}
    @PostMapping("/api/v1/public/shops/{shopCode}/uploads") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobDtos.UploadView> upload(@PathVariable String shopCode,@RequestPart("file") MultipartFile file){return ApiResponse.ok("Upload stored temporarily. No print or payment was initiated.",jobs.upload(shopCode,file));}
    @PostMapping("/api/v1/public/shops/{shopCode}/jobs") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobDtos.JobView> create(@PathVariable String shopCode,@Valid @RequestBody JobDtos.CreateRequest request){var shop=shops.findByShopCodeIgnoreCase(shopCode).filter(Shop::isActive).orElseThrow(()->com.printdoc.api.common.ApiException.notFound("Shop was not found."));return ApiResponse.ok("Quote created; payment and printing are not connected.",jobs.createForShop(shop,request));}
    @GetMapping("/api/v1/public/jobs/{jobId}/status") public ApiResponse<JobDtos.JobView> status(@PathVariable String jobId){return ApiResponse.ok("Job status.",jobs.publicStatus(jobId));}
    @GetMapping("/api/v1/owner/jobs") @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ApiResponse<PageResponse<JobDtos.JobView>> list(HttpServletRequest request,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) PrintJob.Status status){if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page or page size.");return ApiResponse.ok("Shop jobs.",jobs.list(current.requireShopId(request),status,page,size));}
    @GetMapping("/api/v1/owner/jobs/{jobId}") @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')") public ApiResponse<JobDtos.JobView> get(HttpServletRequest request,@PathVariable String jobId){return ApiResponse.ok("Shop job.",jobs.getForOwner(jobId,current.requireShopId(request)));}
    @PostMapping("/api/v1/owner/jobs/{jobId}/transition") @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')") public ApiResponse<JobDtos.JobView> transition(HttpServletRequest request,@PathVariable String jobId,@Valid @RequestBody JobDtos.StatusUpdate update){var user=current.require(request);var result=jobs.transitionOwner(jobId,user.getShop().getId(),update);socket.publishJobStatus(user.getShop().getShopCode(),result);if(result.status().equals(PrintJob.Status.QUEUED.name()))socket.dispatch(user.getShop().getShopCode(),result);return ApiResponse.ok("Job state updated.",result);}
}
