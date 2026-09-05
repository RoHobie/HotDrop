package com.hotdrop.admin;

import com.hotdrop.admin.dto.EventSalesResponse;
import com.hotdrop.admin.dto.PlatformSalesSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminSalesController {

    private final AdminSalesService adminSalesService;

    public AdminSalesController(AdminSalesService adminSalesService) {
        this.adminSalesService = adminSalesService;
    }

    @GetMapping("/events/{id}/sales")
    public ResponseEntity<EventSalesResponse> getEventSales(@PathVariable Long id) {
        EventSalesResponse response = adminSalesService.getEventSales(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sales/summary")
    public ResponseEntity<PlatformSalesSummaryResponse> getSalesSummary() {
        PlatformSalesSummaryResponse response = adminSalesService.getPlatformSalesSummary();
        return ResponseEntity.ok(response);
    }
}
