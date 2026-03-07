package ngo.cong.thao.s2o_pro.table.controller;

import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.table.entity.DiningTable;
import ngo.cong.thao.s2o_pro.table.service.DiningTableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tables")
public class DiningTableController {

    private final DiningTableService tableService;

    public DiningTableController(DiningTableService tableService) {
        this.tableService = tableService;
    }

    // Chỉ Chủ quán hoặc Quản lý mới được tạo Bàn mới
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<DiningTable>> createTable(@RequestParam String name) {
        DiningTable table = tableService.createTable(name);
        return ResponseEntity.ok(ApiResponse.success(table));
    }
}