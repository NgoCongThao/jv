package ngo.cong.thao.s2o_pro.user.service;

import ngo.cong.thao.s2o_pro.user.dto.StaffRequest;
import ngo.cong.thao.s2o_pro.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    User createStaff(StaffRequest request);
    List<User> getStaffList();
    User toggleStaffStatus(UUID id);
}