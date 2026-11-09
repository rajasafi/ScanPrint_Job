package com.printdoc.api.device;

import com.printdoc.api.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class DeviceController {
    private final DeviceService devices;
    public DeviceController(DeviceService devices){this.devices=devices;}

    /** Public polling endpoint used by a newly installed agent before owner approval. */
    @GetMapping({"/api/v1/devices/check-status","/api/devices/check-status"})
    public ApiResponse<DeviceDtos.StatusView> checkStatus(@RequestParam @NotBlank @Size(max=32) String shopCode,@RequestParam @NotBlank @Size(max=128) String fingerprint){
        return ApiResponse.ok("Device approval status.",devices.checkStatus(shopCode,fingerprint));
    }
}
