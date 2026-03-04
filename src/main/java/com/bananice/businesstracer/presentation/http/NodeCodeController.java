package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.infrastructure.registry.BusinessTraceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for getting registered node codes
 */
@RestController
@RequestMapping("/business-tracer/api")
public class NodeCodeController {

    @Resource
    private BusinessTraceRegistry registry;

    /**
     * Get all registered @BusinessTrace node codes
     * These are scanned from code annotations at startup
     */
    @GetMapping("/node-codes")
    public ResponseEntity<Map<String, Object>> getNodeCodes() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", registry.getAllNodes());
        return ResponseEntity.ok(result);
    }
}
