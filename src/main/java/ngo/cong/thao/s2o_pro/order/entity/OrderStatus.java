package ngo.cong.thao.s2o_pro.order.entity;

public enum OrderStatus {
    NEW,                // Vừa tạo
    CONFIRMED,          // Đã xác nhận
    COOKING,            // Đang nấu (DINE_IN)
    PREPARING,          // Đang chuẩn bị (DELIVERY)
    READY,              // Chuẩn bị xong (DELIVERY)
    DONE,               // Đã lên món (DINE_IN)
    OUT_FOR_DELIVERY,   // Đang giao (DELIVERY)
    DELIVERED,          // Đã giao tới (DELIVERY)
    PAID,
    PENDING_PAYMENT,// Đã thanh toán (Hoàn thành)
    CANCELLED           // Hủy đơn
}