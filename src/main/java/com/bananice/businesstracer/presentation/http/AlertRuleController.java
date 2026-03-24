package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.application.alert.AlertConfigCacheService;
import com.bananice.businesstracer.application.dto.alert.AlertRuleUpsertRequest;
import com.bananice.businesstracer.domain.model.alert.AlertRule;
import com.bananice.businesstracer.domain.model.alert.AlertScopeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.validation.Valid;

@RestController
@RequestMapping("/business-tracer/api/alerts/rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertConfigCacheService alertConfigCacheService;

    @GetMapping
    public ResponseEntity<ApiResult<List<AlertRule>>> listRules() {
        return ResponseEntity.ok(ApiResult.success(alertConfigCacheService.listAllRules()));
    }

    @PutMapping("/{scopeType}/{scopeCode}")
    public ResponseEntity<ApiResult<Void>> upsertRule(@PathVariable String scopeType,
                                                      @PathVariable String scopeCode,
                                                      @Valid @RequestBody AlertRuleUpsertRequest request) {
        AlertScopeType parsedScopeType;
        try {
            parsedScopeType = AlertScopeType.valueOf(scopeType);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResult.error(400, "invalid scopeType: " + scopeType));
        }

        if (parsedScopeType == AlertScopeType.NODE && (request == null || request.getFlowCode() == null || request.getFlowCode().trim().isEmpty())) {
            return ResponseEntity.ok(ApiResult.error(400, "flowCode is required when scopeType is NODE"));
        }

        if (request == null) {
            return ResponseEntity.ok(ApiResult.error(400, "request body is required"));
        }

        AlertRule rule = AlertRule.builder()
                .name(request.getName())
                .alertType(request.getAlertType())
                .scopeType(parsedScopeType)
                .scopeRef(scopeCode)
                .flowCode(resolveFlowCode(parsedScopeType, scopeCode, request.getFlowCode()))
                .enabled(request.getEnabled())
                .build();
        alertConfigCacheService.saveRule(rule);
        return ResponseEntity.ok(ApiResult.success(null, "rule saved"));
    }

    private String resolveFlowCode(AlertScopeType scopeType, String scopeCode, String requestFlowCode) {
        if (scopeType == AlertScopeType.GLOBAL) {
            return null;
        }
        if (scopeType == AlertScopeType.FLOW) {
            return scopeCode;
        }
        return requestFlowCode;
    }
}
