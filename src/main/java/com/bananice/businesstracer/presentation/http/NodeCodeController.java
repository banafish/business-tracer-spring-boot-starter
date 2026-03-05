package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.infrastructure.registry.BusinessTraceRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * REST Controller for getting registered node codes
 */
@RestController
@RequestMapping("/business-tracer/api")
@RequiredArgsConstructor
public class NodeCodeController {

    private final BusinessTraceRegistry registry;

    /**
     * Get all registered @BusinessTrace node codes
     * These are scanned from code annotations at startup
     */
    @GetMapping("/node-codes")
    public ResponseEntity<ApiResult<Collection<BusinessTraceRegistry.NodeInfo>>> getNodeCodes() {
        return ResponseEntity.ok(ApiResult.success(registry.getAllNodes()));
    }
}
