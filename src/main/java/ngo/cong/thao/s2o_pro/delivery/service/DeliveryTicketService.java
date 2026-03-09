package ngo.cong.thao.s2o_pro.delivery.service;

import ngo.cong.thao.s2o_pro.delivery.dto.DeliveryTicketUpdateRequest;
import ngo.cong.thao.s2o_pro.delivery.entity.DeliveryTicket;
import java.util.UUID;

public interface DeliveryTicketService {
    DeliveryTicket updateShipperInfo(UUID ticketId, DeliveryTicketUpdateRequest request);
    DeliveryTicket updateTicketStatus(UUID ticketId, DeliveryTicket.TicketStatus newStatus);
}