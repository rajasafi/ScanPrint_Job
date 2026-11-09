package com.printdoc.api.device;

import com.printdoc.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private final DeviceService devices;
    public AgentController(DeviceService devices){this.devices=devices;}
    /** REST fallback for the desktop agent while a Socket.IO gateway is not deployed. */
    @PostMapping("/register")
    public ApiResponse<DeviceDtos.DeviceView> register(@Valid @RequestBody DeviceDtos.AgentRegisterRequest request,@RequestHeader(value="X-Secret-Api-Key",required=false) String secretApiKey){
        return ApiResponse.ok("Agent registered.",devices.register(request,secretApiKey));
    }
    @PostMapping("/heartbeat")
    public ApiResponse<DeviceDtos.DeviceView> heartbeat(@Valid @RequestBody DeviceDtos.AgentHeartbeatRequest request,@RequestHeader(value="X-Secret-Api-Key",required=false) String secretApiKey){
        return ApiResponse.ok("Agent heartbeat accepted.",devices.heartbeat(request,secretApiKey));
    }
}
