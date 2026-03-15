package ngo.cong.thao.s2o_pro.tenant.dto;

import lombok.Data;

@Data
public class TenantRegisterRequest {
    private String restaurantName;
    private String domain;
    private String address;
    private String phone;
    private String ownerEmail;
}