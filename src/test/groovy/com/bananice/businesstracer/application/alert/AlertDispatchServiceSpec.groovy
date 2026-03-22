package com.bananice.businesstracer.application.alert

import com.bananice.businesstracer.domain.model.alert.AlertChannel
import com.bananice.businesstracer.domain.model.alert.AlertChannelType
import com.bananice.businesstracer.domain.model.alert.AlertDispatchLog
import com.bananice.businesstracer.domain.model.alert.AlertEvent
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertChannelRepository
import com.bananice.businesstracer.domain.repository.alert.AlertDispatchLogRepository
import com.bananice.businesstracer.infrastructure.alert.channel.AlertChannelSender
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime
import java.time.LocalTime

class AlertDispatchServiceSpec extends Specification {

    AlertChannelRepository alertChannelRepository = Mock()
    AlertDispatchLogRepository alertDispatchLogRepository = Mock()
    AlertChannelSender webhookSender = Mock()
    AlertChannelSender emailSender = Mock()

    @Subject
    AlertDispatchService alertDispatchService = new AlertDispatchService(
            alertChannelRepository,
            alertDispatchLogRepository,
            [webhookSender, emailSender],
            80L,
            1
    )

    private AlertChannel buildChannel(Map overrides = [:]) {
        def defaults = [
                id         : 1L,
                name       : "default-channel",
                channelType: AlertChannelType.WEBHOOK,
                target     : "http://example",
                enabled    : true
        ]
        def merged = defaults + overrides

        AlertChannel.builder()
                .id(merged.id as Long)
                .name(merged.name as String)
                .channelType(merged.channelType as AlertChannelType)
                .target(merged.target as String)
                .enabled(merged.enabled as Boolean)
                .build()
    }

    private AlertEvent buildEvent() {
        AlertEvent.builder()
                .id(301L)
                .alertType(AlertType.NODE_FAILED)
                .status(AlertStatus.NEW)
                .businessId("biz-1")
                .flowCode("flow-a")
                .nodeCode("node-a")
                .traceId("trace-1")
                .message("node failed")
                .occurredAt(LocalDateTime.of(2026, 3, 22, 10, 0))
                .build()
    }

    def "dispatch retries within strict upper bound isolates channel failures and logs each attempt"() {
        given:
        def webhook = buildChannel(id: 1L, channelType: AlertChannelType.WEBHOOK)
        def email = buildChannel(id: 2L, channelType: AlertChannelType.EMAIL)

        and:
        alertChannelRepository.findEnabled() >> [webhook, email]
        webhookSender.supports(AlertChannelType.WEBHOOK) >> true
        webhookSender.supports(AlertChannelType.EMAIL) >> false
        emailSender.supports(AlertChannelType.WEBHOOK) >> false
        emailSender.supports(AlertChannelType.EMAIL) >> true

        when:
        alertDispatchService.dispatchRealtime(buildEvent())

        then: "webhook attempts are capped by retry upper bound (1 retry => 2 attempts)"
        2 * webhookSender.send(webhook, _ as AlertEvent) >> { throw new RuntimeException("boom") }

        and: "email channel still dispatches despite webhook failures"
        1 * emailSender.send(email, _ as AlertEvent) >> "ok"

        and: "a dispatch log is written for each attempt"
        3 * alertDispatchLogRepository.save(_)
    }

    def "dispatch timeout is bounded by retry cap and next channel still executes"() {
        given:
        def webhook = buildChannel(id: 11L, channelType: AlertChannelType.WEBHOOK)
        def email = buildChannel(id: 12L, channelType: AlertChannelType.EMAIL)
        def savedLogs = []

        and:
        alertChannelRepository.findEnabled() >> [webhook, email]
        webhookSender.supports(AlertChannelType.WEBHOOK) >> true
        webhookSender.supports(AlertChannelType.EMAIL) >> false
        emailSender.supports(AlertChannelType.WEBHOOK) >> false
        emailSender.supports(AlertChannelType.EMAIL) >> true

        and: "capture all dispatch logs"
        alertDispatchLogRepository.save(_ as AlertDispatchLog) >> { AlertDispatchLog log ->
            savedLogs << log
            log
        }

        when:
        alertDispatchService.dispatchRealtime(buildEvent())

        then: "retry is capped to 2 attempts total when maxRetries=1"
        2 * webhookSender.send(webhook, _ as AlertEvent) >> {
            Thread.sleep(120L)
            "late-response"
        }

        and: "next channel still executes after webhook timeout failures"
        1 * emailSender.send(email, _ as AlertEvent) >> "ok"

        and: "failed timeout attempts and successful isolation are both logged"
        savedLogs.size() == 3
        savedLogs.count { it.channelId == 11L && it.status == AlertStatus.FAILED && it.response?.contains("attempt timeout") } == 2
        savedLogs.count { it.channelId == 12L && it.status == AlertStatus.SENT } == 1
    }

    def "silence window supports cross midnight and edge boundaries"() {
        expect:
        alertDispatchService.isWithinSilenceWindow(LocalTime.of(23, 0), LocalTime.of(23, 0), LocalTime.of(2, 0))
        alertDispatchService.isWithinSilenceWindow(LocalTime.of(1, 59), LocalTime.of(23, 0), LocalTime.of(2, 0))
        !alertDispatchService.isWithinSilenceWindow(LocalTime.of(2, 0), LocalTime.of(23, 0), LocalTime.of(2, 0))

        and:
        alertDispatchService.isWithinSilenceWindow(LocalTime.of(10, 0), LocalTime.of(10, 0), LocalTime.of(12, 0))
        !alertDispatchService.isWithinSilenceWindow(LocalTime.of(12, 0), LocalTime.of(10, 0), LocalTime.of(12, 0))
    }
}
