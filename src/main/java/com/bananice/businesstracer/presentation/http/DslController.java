package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.application.DslService;
import com.bananice.businesstracer.domain.model.DslConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for DSL CRUD operations
 */
@RestController
@RequestMapping("/business-tracer/api/dsl")
@RequiredArgsConstructor
public class DslController {

    private final DslService dslService;

    /**
     * Get all DSL configurations
     */
    @GetMapping
    public ResponseEntity<ApiResult<List<DslConfig>>> listDsl() {
        List<DslConfig> list = dslService.getAllDsl();
        return ResponseEntity.ok(ApiResult.success(list));
    }

    /**
     * Get DSL by flowCode
     */
    @GetMapping("/{flowCode}")
    public ResponseEntity<ApiResult<DslConfig>> getDsl(@PathVariable String flowCode) {
        DslConfig dsl = dslService.getDslByFlowCode(flowCode);
        if (dsl != null) {
            return ResponseEntity.ok(ApiResult.success(dsl));
        } else {
            return ResponseEntity.ok(ApiResult.error(404, "DSL not found: " + flowCode));
        }
    }

    /**
     * Create a new DSL
     */
    @PostMapping
    public ResponseEntity<ApiResult<DslConfig>> createDsl(@RequestBody DslConfig dslConfig) {
        try {
            DslConfig created = dslService.createDsl(dslConfig);
            return ResponseEntity.ok(ApiResult.success(created, "DSL created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResult.error(400, e.getMessage()));
        }
    }

    /**
     * Update an existing DSL
     */
    @PutMapping("/{flowCode}")
    public ResponseEntity<ApiResult<DslConfig>> updateDsl(
            @PathVariable String flowCode,
            @RequestBody DslConfig dslConfig) {
        try {
            DslConfig updated = dslService.updateDsl(flowCode, dslConfig);
            return ResponseEntity.ok(ApiResult.success(updated, "DSL updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResult.error(404, e.getMessage()));
        }
    }

    /**
     * Delete a DSL
     */
    @DeleteMapping("/{flowCode}")
    public ResponseEntity<ApiResult<Void>> deleteDsl(@PathVariable String flowCode) {
        boolean deleted = dslService.deleteDsl(flowCode);
        if (deleted) {
            return ResponseEntity.ok(ApiResult.success(null, "DSL deleted successfully"));
        } else {
            return ResponseEntity.ok(ApiResult.error(404, "DSL not found: " + flowCode));
        }
    }
}
