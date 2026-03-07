package ngo.cong.thao.s2o_pro.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ngo.cong.thao.s2o_pro.order.entity.PaymentMethod;
import java.math.BigDecimal;

@Data
public class OrderPayRequest {
    @NotNull(message = "Vui lòng chọn hình thức thanh toán")
    private PaymentMethod paymentMethod;

    // Nếu khách trả tiền mặt thì nhập vào đây, chuyển khoản thì bỏ trống
    private BigDecimal amountGiven;
}