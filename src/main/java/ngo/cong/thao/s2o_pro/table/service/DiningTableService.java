package ngo.cong.thao.s2o_pro.table.service;

import ngo.cong.thao.s2o_pro.table.entity.DiningTable;

public interface DiningTableService {
    DiningTable createTable(String tableName);
    ngo.cong.thao.s2o_pro.table.entity.TableReservation reserveTable(java.util.UUID tableId, String customerName, String phone, java.time.LocalDateTime time, int guestCount);
}