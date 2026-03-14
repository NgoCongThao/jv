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

    @org.springframework.beans.factory.annotation.Value("${vnpay.tmn-code}")
    private String vnp_TmnCode;

    @org.springframework.beans.factory.annotation.Value("${vnpay.hash-secret}")
    private String vnp_HashSecret;

    @org.springframework.beans.factory.annotation.Value("${vnpay.pay-url}")
    private String vnp_PayUrl;

    @org.springframework.beans.factory.annotation.Value("${vnpay.return-url}")
    private String vnp_ReturnUrl;
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
    public ResponseEntity<Void> vnpayReturn(jakarta.servlet.http.HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");

        // ĐỊA CHỈ TRANG WEB CỦA ANH (SAU NÀY LÀM FRONTEND SẼ TRỎ VỀ ĐÂY)
        // Hiện tại để test, mình sẽ chuyển tạm nó về 1 link ảo trên localhost
        String FRONTEND_SUCCESS_URL = "http://localhost:3000/order-success";
        String FRONTEND_FAIL_URL = "http://localhost:3000/order-fail";

        if ("00".equals(responseCode)) {
            try {
                String uuidStr = txnRef.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                UUID orderId = UUID.fromString(uuidStr);

                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

                if (order.getStatus() == ngo.cong.thao.s2o_pro.order.entity.OrderStatus.PENDING_PAYMENT) {
                    orderService.updateOrderStatus(orderId, ngo.cong.thao.s2o_pro.order.entity.OrderStatus.NEW);
                } else {
                    orderService.updateOrderStatus(orderId, ngo.cong.thao.s2o_pro.order.entity.OrderStatus.PAID);
                }

                // 🌟 THAY VÌ TRẢ VỀ HTML -> CHUYỂN HƯỚNG BẮT TRÌNH DUYỆT NHẢY VỀ FRONTEND
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(java.net.URI.create(FRONTEND_SUCCESS_URL + "?orderId=" + orderId))
                        .build();

            } catch (Exception e) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(java.net.URI.create(FRONTEND_FAIL_URL + "?error=" + e.getMessage()))
                        .build();
            }
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create(FRONTEND_FAIL_URL + "?vnp_ResponseCode=" + responseCode))
                    .build();
        }
    }
}