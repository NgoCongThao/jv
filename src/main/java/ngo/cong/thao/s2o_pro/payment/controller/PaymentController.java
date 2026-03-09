package ngo.cong.thao.s2o_pro.payment.controller;

import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.repository.OrderRepository;
import ngo.cong.thao.s2o_pro.payment.util.VNPayUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final OrderRepository orderRepository;
    private final ngo.cong.thao.s2o_pro.order.service.OrderService orderService;

    // ĐÃ FIX: Thêm tham số orderService vào trong ngoặc
    public PaymentController(OrderRepository orderRepository, ngo.cong.thao.s2o_pro.order.service.OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }
    @PostMapping("/vnpay/create-url/{orderId}")
    public ResponseEntity<ApiResponse<String>> createVNPayUrl(@PathVariable UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // ==========================================
        // 1. HARDCODE TRỰC TIẾP ĐỂ TRÁNH LỖI FILE YML
        // ==========================================
        String vnp_TmnCode = "BQZV6E3R";
        String vnp_HashSecret = "JG0QD4Y9IXN0XWAFWIWCXTOUJB96Y03B";
        String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String vnp_ReturnUrl = "http://localhost:8080/api/payment/vnpay/return";

        long amount = order.getTotalAmount().longValue() * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        // Tuyệt đối không có dấu cách hay ký tự đặc biệt
        String txnRef = order.getId().toString().replace("-", "");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh_toan_don_hang_" + txnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        // ==========================================
        // 2. VÒNG LẶP CHUẨN SDK VNPAY JAVA (KHÔNG CHẾ CHÁO)
        // ==========================================
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);

            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnp_PayUrl + "?" + queryUrl;

        System.out.println("====== KIỂM TRA BẢO MẬT ======");
        System.out.println("Độ dài HashSecret: " + vnp_HashSecret.length());
        System.out.println("HashData: " + hashData.toString());
        System.out.println("===============================");

        return ResponseEntity.ok(ApiResponse.success(paymentUrl));
    }
    @GetMapping("/vnpay/return")
    public ResponseEntity<String> vnpayReturn(jakarta.servlet.http.HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");

        // Mã "00" của VNPay nghĩa là Giao dịch Thành công
        if ("00".equals(responseCode)) {
            try {
                // Lúc gửi đi mình đã xóa dấu gạch ngang (hyphen) của UUID, giờ phải nối nó lại
                String uuidStr = txnRef.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                UUID orderId = UUID.fromString(uuidStr);

                // MA THUẬT NẰM Ở ĐÂY: Cập nhật đơn hàng thành PAID
                // Lập tức Event-Driven sẽ kích hoạt Robot đi dọn bàn (chuyển bàn về AVAILABLE)
                orderService.updateOrderStatus(orderId, ngo.cong.thao.s2o_pro.order.entity.OrderStatus.PAID);

                return ResponseEntity.ok(
                        "<div style='text-align:center; margin-top:50px; font-family: Arial;'>" +
                                "<h1 style='color: green;'>✅ GIAO DỊCH THÀNH CÔNG!</h1>" +
                                "<h3>Đơn hàng của bạn đã được thanh toán. Bàn đã được dọn tự động!</h3>" +
                                "<p>Mã đơn: " + orderId + "</p>" +
                                "</div>"
                );
            } catch (Exception e) {
                return ResponseEntity.ok("<h1>GIAO DỊCH THÀNH CÔNG!</h1><p>Nhưng có lỗi khi cập nhật vào hệ thống: " + e.getMessage() + "</p>");
            }
        } else {
            return ResponseEntity.status(400).body(
                    "<div style='text-align:center; margin-top:50px; font-family: Arial;'>" +
                            "<h1 style='color: red;'>❌ GIAO DỊCH THẤT BẠI!</h1>" +
                            "<p>Mã lỗi từ VNPay: " + responseCode + "</p>" +
                            "</div>"
            );
        }
    }
}