package ngo.cong.thao.s2o_pro.user.entity;

public enum Role {
    // Cấp độ Nền tảng (SaaS Platform)
    ADMIN,          // Quản trị viên toàn hệ thống, phê duyệt Tenant

    // Cấp độ Nhà hàng (Tenant Level)
    OWNER,          // Chủ nhà hàng, có toàn quyền trên dữ liệu của mình
    MANAGER,        // Quản lý cửa hàng, quản lý thực đơn và nhân sự
    CHEF,           // Nhân viên bếp/bar, xử lý món ăn
    CASHIER,        // Thu ngân, xử lý thanh toán và hóa đơn

    // Cấp độ Người dùng (User Level)
    CUSTOMER,       // Khách hàng có tài khoản, sử dụng App Mobile
    GUEST           // Khách vãng lai quét mã tại bàn, định danh theo Session
}