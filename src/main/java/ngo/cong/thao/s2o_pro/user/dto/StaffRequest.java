package ngo.cong.thao.s2o_pro.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ngo.cong.thao.s2o_pro.user.entity.Role;

@Data
public class StaffRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String usernamePrefix; // Ví dụ nhập "vy", Backend sẽ tự nối thành "katinat_vy"

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotNull(message = "Chức vụ không được để trống")
    private Role role;
}