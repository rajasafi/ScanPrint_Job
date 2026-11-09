package com.printdoc.api.setup;

import com.printdoc.api.common.ApiResponse;
import com.printdoc.api.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner")
public class SetupController {
    private final SetupService setup;
    private final CurrentUser current;

    public SetupController(SetupService setup, CurrentUser current) {
        this.setup = setup;
        this.current = current;
    }

    @GetMapping("/setup-status")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ApiResponse<SetupDtos.Status> status(HttpServletRequest request) {
        return ApiResponse.ok("Current setup readiness.", setup.status(current.requireShopId(request)));
    }
}
