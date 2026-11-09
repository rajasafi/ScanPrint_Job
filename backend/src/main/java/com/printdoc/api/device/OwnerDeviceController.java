package com.printdoc.api.device;

import com.printdoc.api.common.ApiResponse;
import com.printdoc.api.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/owner/devices","/api/v1/devices"})
@PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
public class OwnerDeviceController {
    private final DeviceService devices; private final CurrentUser current;
    public OwnerDeviceController(DeviceService devices,CurrentUser current){this.devices=devices;this.current=current;}
    @GetMapping({"","/my-devices"})
    public ApiResponse<DeviceDtos.DeviceList> list(HttpServletRequest request){return ApiResponse.ok("Registered devices.",devices.ownerDevices(current.require(request)));}
    @PostMapping("/credentials")
    public ApiResponse<DeviceDtos.CredentialView> issueCredentials(HttpServletRequest request){return ApiResponse.ok("Agent secret issued. Store it securely; it will not be shown again.",devices.issueCredentials(current.require(request)));}
    @PostMapping("/{deviceId}/approve")
    public ApiResponse<DeviceDtos.DeviceView> approve(HttpServletRequest request,@PathVariable UUID deviceId){return ApiResponse.ok("Device approved.",devices.approve(current.require(request),deviceId));}
    @PostMapping("/{deviceId}/reject")
    public ApiResponse<DeviceDtos.DeviceView> reject(HttpServletRequest request,@PathVariable UUID deviceId){return ApiResponse.ok("Device rejected.",devices.reject(current.require(request),deviceId));}
    @PostMapping("/{deviceId}/revoke")
    public ApiResponse<DeviceDtos.DeviceView> revoke(HttpServletRequest request,@PathVariable UUID deviceId){return ApiResponse.ok("Device revoked.",devices.revoke(current.require(request),deviceId));}
}
