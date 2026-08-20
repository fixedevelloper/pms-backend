package com.pms.hotel.loyalty.internal.web;

import com.pms.hotel.loyalty.LoyaltyAccountView;
import com.pms.hotel.loyalty.LoyaltyTransactionView;
import com.pms.hotel.loyalty.internal.LoyaltyService;
import com.pms.hotel.loyalty.internal.web.LoyaltyRequests.AdjustPointsRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty/guests/{guestId}")
@RequiredArgsConstructor
class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping
    public LoyaltyAccountView show(@PathVariable Long guestId) {
        return loyaltyService.getAccount(guestId);
    }

    @GetMapping("/transactions")
    public List<LoyaltyTransactionView> transactions(@PathVariable Long guestId) {
        return loyaltyService.listTransactions(guestId);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN', 'MANAGER')")
    public LoyaltyAccountView adjust(@PathVariable Long guestId, @Valid @RequestBody AdjustPointsRequest request) {
        return loyaltyService.adjustPoints(guestId, request.points(), request.description());
    }
}
