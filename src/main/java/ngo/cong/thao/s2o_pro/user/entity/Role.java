package ngo.cong.thao.s2o_pro.user.entity;

public enum Role {
    PLATFORM_ADMIN,   // Quản trị viên cao nhất của hệ thống S2O
    RESTAURANT_OWNER, // Chủ nhà hàng
    CHEF,             // Đầu bếp
    CASHIER,          // Thu ngân
    CUSTOMER,         // Khách hàng có tài khoản
    GUEST             // Khách vãng lai (quét QR, không cần tài khoản)
}