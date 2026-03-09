package ngo.cong.thao.s2o_pro.delivery.controller;

import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.delivery.dto.DeliveryTicketUpdateRequest;
import ngo.cong.thao.s2o_pro.delivery.entity.DeliveryTicket;
import ngo.cong.thao.s2o_pro.delivery.service.DeliveryTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/delivery")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')") // Chỉ nhân viên quán mới được điều phối
public class DeliveryController {

    private final DeliveryTicketService deliveryTicketService;

    public DeliveryController(DeliveryTicketService deliveryTicketService) {
        this.deliveryTicketService = deliveryTicketService;
    }

    // API 1: Lễ tân nhập thông tin Shipper (Grab/ShopeeFood đến nhận)
    @PutMapping("/tickets/{id}/shipper")
    public ResponseEntity<ApiResponse<DeliveryTicket>> assignShipper(
            @PathVariable UUID id,
            @RequestBody DeliveryTicketUpdateRequest request) {

        DeliveryTicket updatedTicket = deliveryTicketService.updateShipperInfo(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedTicket));
    }

    // API 2: Cập nhật trạng thái giao hàng (ON_THE_WAY, DELIVERED)
    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<ApiResponse<DeliveryTicket>> updateStatus(
            @PathVariable UUID id,
            @RequestParam DeliveryTicket.TicketStatus status) {

        DeliveryTicket updatedTicket = deliveryTicketService.updateTicketStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(updatedTicket));
    }
}