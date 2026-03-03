package ngo.cong.thao.s2o_pro.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String username;
    private String role;
    private String tenantId;
}