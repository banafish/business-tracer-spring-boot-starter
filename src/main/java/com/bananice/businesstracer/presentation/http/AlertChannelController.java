package com.bananice.businesstracer.presentation.http;

import com.bananice.businesstracer.application.alert.AlertDispatchService;
import com.bananice.businesstracer.application.dto.alert.AlertChannelUpsertRequest;
import com.bananice.businesstracer.domain.model.alert.AlertChannel;
import com.bananice.businesstracer.domain.model.alert.AlertEvent;
import com.bananice.businesstracer.domain.model.alert.AlertStatus;
import com.bananice.businesstracer.domain.model.alert.AlertType;
import com.bananice.businesstracer.domain.repository.alert.AlertChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

@RestController
@RequestMapping("/business-tracer/api/alerts/channels")
@RequiredArgsConstructor
public class AlertChannelController {

    private final AlertChannelRepository alertChannelRepository;
    private final AlertDispatchService alertDispatchService;

    @GetMapping
    public ResponseEntity<ApiResult<List<AlertChannel>>> listChannels() {
        return ResponseEntity.ok(ApiResult.success(alertChannelRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResult<Void>> createChannel(@Valid @RequestBody AlertChannelUpsertRequest request) {
        if (request == null) {
            return ResponseEntity.ok(ApiResult.error(400, "request body is required"));
        }
        AlertChannel channel = AlertChannel.builder()
                .name(request.getName())
                .channelType(request.getChannelType())
                .target(request.getTarget())
                .enabled(request.getEnabled())
                .build();
        alertChannelRepository.save(channel);
        return ResponseEntity.ok(ApiResult.success(null, "channel created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> updateChannel(@PathVariable Long id,
                                                         @Valid @RequestBody AlertChannelUpsertRequest request) {
        if (request == null) {
            return ResponseEntity.ok(ApiResult.error(400, "request body is required"));
        }
        AlertChannel channel = AlertChannel.builder()
                .id(id)
                .name(request.getName())
                .channelType(request.getChannelType())
                .target(request.getTarget())
                .enabled(request.getEnabled())
                .build();
        alertChannelRepository.save(channel);
        return ResponseEntity.ok(ApiResult.success(null, "channel updated"));
    }

    @PostMapping("/{id}/test-send")
    public ResponseEntity<ApiResult<Void>> testSend(@PathVariable Long id) {
        AlertChannel channel = alertChannelRepository.findById(id);
        if (channel == null) {
            return ResponseEntity.ok(ApiResult.error(404, "channel not found: " + id));
        }
        AlertEvent testEvent = AlertEvent.builder()
                .alertType(AlertType.NODE_FAILED)
                .status(AlertStatus.NEW)
                .flowCode("TEST_FLOW")
                .nodeCode("TEST_NODE")
                .businessId("TEST_BIZ")
                .aggregateKey("TEST:CHANNEL:" + id)
                .message("test alert for channel " + channel.getName())
                .occurredAt(LocalDateTime.now())
                .build();
        alertDispatchService.dispatchToChannel(testEvent, channel);
        return ResponseEntity.ok(ApiResult.success(null, "test alert sent"));
    }
}
