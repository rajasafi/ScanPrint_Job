package com.printdoc.api.plan;

import com.printdoc.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/v1/public/plans")
public class PlanController {
    private final PlanService service;
    public PlanController(PlanService service){this.service=service;}
    @GetMapping public ApiResponse<List<PlanDtos.PlanView>> catalog(){return ApiResponse.ok("Active sample plan catalog. No subscription billing is connected.",service.catalog());}
}
