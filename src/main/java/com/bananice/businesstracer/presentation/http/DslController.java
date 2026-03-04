package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.application.DslService;
import com.bananice.businesstracer.domain.model.DslConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for DSL CRUD operations
 */
@RestController
@RequestMapping("/business-tracer/api/dsl")
public class DslController {

    @Resource
    private DslService dslService;

    /**
     * Get all DSL configurations
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listDsl() {
        List<DslConfig> list = dslService.getAllDsl();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", list);
        return ResponseEntity.ok(result);
    }

    /**
     * Get DSL by flowCode
     */
    @GetMapping("/{flowCode}")
    public ResponseEntity<Map<String, Object>> getDsl(@PathVariable String flowCode) {
        DslConfig dsl = dslService.getDslByFlowCode(flowCode);
        Map<String, Object> result = new HashMap<>();
        if (dsl != null) {
            result.put("code", 200);
            result.put("data", dsl);
        } else {
            result.put("code", 404);
            result.put("message", "DSL not found: " + flowCode);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new DSL
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDsl(@RequestBody DslConfig dslConfig) {
        Map<String, Object> result = new HashMap<>();
        try {
            DslConfig created = dslService.createDsl(dslConfig);
            result.put("code", 200);
            result.put("data", created);
            result.put("message", "DSL created successfully");
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Update an existing DSL
     */
    @PutMapping("/{flowCode}")
    public ResponseEntity<Map<String, Object>> updateDsl(
            @PathVariable String flowCode,
            @RequestBody DslConfig dslConfig) {
        Map<String, Object> result = new HashMap<>();
        try {
            DslConfig updated = dslService.updateDsl(flowCode, dslConfig);
            result.put("code", 200);
            result.put("data", updated);
            result.put("message", "DSL updated successfully");
        } catch (IllegalArgumentException e) {
            result.put("code", 404);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a DSL
     */
    @DeleteMapping("/{flowCode}")
    public ResponseEntity<Map<String, Object>> deleteDsl(@PathVariable String flowCode) {
        Map<String, Object> result = new HashMap<>();
        boolean deleted = dslService.deleteDsl(flowCode);
        if (deleted) {
            result.put("code", 200);
            result.put("message", "DSL deleted successfully");
        } else {
            result.put("code", 404);
            result.put("message", "DSL not found: " + flowCode);
        }
        return ResponseEntity.ok(result);
    }
}
