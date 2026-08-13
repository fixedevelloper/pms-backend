package com.pms.hotel.nightaudit.internal.web;

import com.pms.hotel.nightaudit.NightAuditRunResult;
import com.pms.hotel.nightaudit.internal.NightAuditService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/night-audit")
@RequiredArgsConstructor
class NightAuditController {

    private final NightAuditService nightAuditService;

    @GetMapping("/status")
    public NightAuditStatusView status() {
        LocalDate businessDate = nightAuditService.currentBusinessDate();
        return new NightAuditStatusView(businessDate, nightAuditService.lastRun().orElse(null));
    }

    @PostMapping("/run")
    public NightAuditRunResult run() {
        return nightAuditService.run();
    }

    @GetMapping("/history")
    public List<NightAuditRunResult> history(@RequestParam(defaultValue = "30") int limit) {
        return nightAuditService.history(limit);
    }

    public record NightAuditStatusView(LocalDate businessDate, NightAuditRunResult lastRun) {
    }
}
