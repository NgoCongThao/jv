package ngo.cong.thao.s2o_pro.user.dto;

import lombok.Builder;
import lombok.Data;
import ngo.cong.thao.s2o_pro.user.entity.User;
import java.util.UUID;

@Data
@Builder
public class StaffResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String role;
    private boolean isActive;

    public static StaffResponse fromEntity(User user) {
        return StaffResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .build();
    }
}