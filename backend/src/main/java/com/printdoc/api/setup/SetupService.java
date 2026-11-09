package com.printdoc.api.setup;

import com.printdoc.api.common.ApiException;
import com.printdoc.api.device.AgentDevice;
import com.printdoc.api.device.AgentDeviceRepository;
import com.printdoc.api.job.PrintJobRepository;
import com.printdoc.api.shop.Shop;
import com.printdoc.api.shop.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SetupService {
    private final ShopRepository shops;
    private final PrintJobRepository jobs;
    private final AgentDeviceRepository devices;

    public SetupService(ShopRepository shops, PrintJobRepository jobs, AgentDeviceRepository devices) {
        this.shops = shops;
        this.jobs = jobs;
        this.devices = devices;
    }

    @Transactional(readOnly = true)
    public SetupDtos.Status status(UUID shopId) {
        Shop shop = shops.findById(shopId).filter(Shop::isActive)
            .orElseThrow(() -> ApiException.notFound("Shop was not found."));
        boolean hasJobs = jobs.countByShopId(shopId) > 0;
        boolean hasApprovedDevice = devices.countByShopIdAndStatus(shopId, AgentDevice.Status.APPROVED) > 0;
        boolean hasDeviceRequest = devices.countByShopIdAndStatus(shopId, AgentDevice.Status.PENDING) > 0;
        List<SetupDtos.Step> steps = List.of(
            step("shop-registration", 1, "Register shop & activate demo", SetupDtos.State.READY,
                "Shop account and public shop code are available.", "shop account"),
            step("desktop-agent", 2, "Download desktop print agent", SetupDtos.State.MISSING,
                "No signed Windows agent or download artifact is included in this repository.", "desktop agent"),
            step("pc-binding", 3, "Bind counter PC and configure agent", hasApprovedDevice ? SetupDtos.State.READY : hasDeviceRequest ? SetupDtos.State.PARTIAL : SetupDtos.State.MISSING,
                hasApprovedDevice ? "An approved agent device is registered." : hasDeviceRequest ? "A device is waiting for owner approval." : "Install the agent and request device approval.", "agent enrollment and heartbeat"),
            step("printer-mapping", 4, "Map B&W, color, and duplex printers", SetupDtos.State.MISSING,
                "The owner UI contains sample routes, but the API has no printer/device model or discovery contract.", "printer registry and agent bridge"),
            step("rates-payments", 5, "Configure rates and payments", SetupDtos.State.PARTIAL,
                "Per-page rates are persisted; payment preferences and a verified UPI/payment webhook are not implemented.", "payment provider and settlement"),
            step("counter-qr", 6, "Download counter QR standee", SetupDtos.State.PARTIAL,
                "A shop code exists, but there is no QR payload/configuration endpoint or generated standee asset.", "QR configuration and asset generation"),
            step("customer-orders", 7, "Accept customer scans and self-orders", hasJobs ? SetupDtos.State.READY : SetupDtos.State.PARTIAL,
                hasJobs ? "Upload and job creation have recorded at least one order." : "Public upload and job endpoints exist, but no order has been recorded for this shop yet.", "public kiosk integration"),
            step("auto-print", 8, "Spool jobs and bring shop live", SetupDtos.State.BLOCKED,
                "Jobs can be queued as API records, but no authenticated agent consumer or printer execution path exists.", "queue worker and printer agent")
        );
        int ready = (int) steps.stream().filter(s -> s.state() == SetupDtos.State.READY).count();
        List<String> gaps = new ArrayList<>();
        steps.stream().filter(s -> s.state() == SetupDtos.State.MISSING || s.state() == SetupDtos.State.BLOCKED)
            .forEach(s -> gaps.add(s.number() + ". " + s.requiredComponent()));
        return new SetupDtos.Status(shop.getShopCode(), ready, steps.size(), steps, gaps);
    }

    private static SetupDtos.Step step(String id, int number, String title, SetupDtos.State state, String note, String component) {
        return new SetupDtos.Step(id, number, title, state, note, component);
    }
}
