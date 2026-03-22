package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.application.dto.PageResult;
import com.bananice.businesstracer.domain.model.alert.AlertDispatchLog;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import com.bananice.businesstracer.domain.repository.alert.AlertDispatchLogRepository;
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/business-tracer/api/alerts/events")
@RequiredArgsConstructor
public class AlertHistoryController {

    private final AlertEventRepository alertEventRepository;
    private final AlertDispatchLogRepository alertDispatchLogRepository;

    @GetMapping
    public ResponseEntity<ApiResult<PageResult<AlertEvent>>> queryEvents(
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "alertType", required = false) AlertType alertType,
            @RequestParam(value = "status", required = false) AlertStatus status,
            @RequestParam(value = "flowCode", required = false) String flowCode,
            @RequestParam(value = "nodeCode", required = false) String nodeCode,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        List<AlertEvent> list = alertEventRepository.query(startTime, endTime, alertType, status, flowCode, nodeCode, businessId, pageNum, pageSize);
        long total = alertEventRepository.count(startTime, endTime, alertType, status, flowCode, nodeCode, businessId);
        return ResponseEntity.ok(ApiResult.success(PageResult.of(total, pageNum, pageSize, list)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<AlertEvent>> getEvent(@PathVariable Long id) {
        AlertEvent event = alertEventRepository.findById(id);
        if (event == null) {
            return ResponseEntity.ok(ApiResult.error(404, "alert event not found: " + id));
        }
        return ResponseEntity.ok(ApiResult.success(event));
    }

    @GetMapping("/{id}/dispatch-logs")
    public ResponseEntity<ApiResult<List<AlertDispatchLog>>> getDispatchLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(alertDispatchLogRepository.findByEventId(id)));
    }
}
