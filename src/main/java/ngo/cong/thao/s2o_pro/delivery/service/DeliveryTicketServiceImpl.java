package ngo.cong.thao.s2o_pro.delivery.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.delivery.dto.DeliveryTicketUpdateRequest;
import ngo.cong.thao.s2o_pro.delivery.entity.DeliveryTicket;
import ngo.cong.thao.s2o_pro.delivery.repository.DeliveryTicketRepository;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;
import ngo.cong.thao.s2o_pro.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryTicketServiceImpl implements DeliveryTicketService {

    private final DeliveryTicketRepository deliveryTicketRepository;
    private final OrderService orderService; // Dùng để đồng bộ ngược lại với Order

    @Override
    @Transactional
    public DeliveryTicket updateShipperInfo(UUID ticketId, DeliveryTicketUpdateRequest request) {
        DeliveryTicket ticket = deliveryTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Vận đơn này"));

        ticket.setShipperName(request.getShipperName());
        ticket.setShipperPhone(request.getShipperPhone());
        ticket.setLicensePlate(request.getLicensePlate());

        if (request.getDeliveryFee() != null) {
            ticket.setDeliveryFee(request.getDeliveryFee());
        }

        // Khi có shipper nhận đơn, tự động chuyển trạng thái vận đơn sang PICKING_UP (Đang đến lấy)
        ticket.setStatus(DeliveryTicket.TicketStatus.PICKING_UP);

        return deliveryTicketRepository.save(ticket);
    }

    @Override
    @Transactional
    public DeliveryTicket updateTicketStatus(UUID ticketId, DeliveryTicket.TicketStatus newStatus) {
        DeliveryTicket ticket = deliveryTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Vận đơn này"));

        ticket.setStatus(newStatus);
        DeliveryTicket savedTicket = deliveryTicketRepository.save(ticket);

        // --- MA THUẬT ĐỒNG BỘ: Cập nhật ngược lại Order ---
        if (newStatus == DeliveryTicket.TicketStatus.ON_THE_WAY) {
            // Shipper bắt đầu đi giao -> Đơn hàng đổi thành OUT_FOR_DELIVERY
            orderService.updateOrderStatus(ticket.getOrderId(), OrderStatus.OUT_FOR_DELIVERY);
        } else if (newStatus == DeliveryTicket.TicketStatus.DELIVERED) {
            // Shipper giao xong -> Đơn hàng đổi thành DELIVERED
            orderService.updateOrderStatus(ticket.getOrderId(), OrderStatus.DELIVERED);
        }

        return savedTicket;
    }
}