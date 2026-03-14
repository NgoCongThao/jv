package ngo.cong.thao.s2o_pro.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT, // Chờ thanh toán online (Delivery)
    NEW,             // Đơn mới
    CONFIRMED,       // Đã xác nhận
    COOKING,         // Đang nấu
    PREPARING,       // Đang chuẩn bị (đồ uống)
    READY,           // Chờ phục vụ / Chờ giao
    SERVED,          // Đã phục vụ tại bàn
    OUT_FOR_DELIVERY,// Đang giao hàng
    DELIVERED,       // Đã giao thành công
    DONE,            // Khách đã dùng xong
    PAYMENT_REQUESTED, // ---> THÊM MỚI: Khách đang gọi tính tiền
    PAID,            // Đã thanh toán
    CANCELLED        // Đã hủy
}