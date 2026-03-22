package com.bananice.businesstracer.infrastructure.persistence.alert

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.domain.model.alert.AlertChannel
import com.bananice.businesstracer.domain.model.alert.AlertChannelType
import com.bananice.businesstracer.domain.repository.alert.AlertChannelRepository
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.Resource

@SpringBootTest(classes = TestApplication)
@Transactional
class AlertChannelRepositoryImplSpec extends Specification {

    @Resource
    @Subject
    AlertChannelRepository alertChannelRepository

    private AlertChannel buildChannel(Map overrides = [:]) {
        def defaults = [
                id         : null,
                name       : "channel-default",
                channelType: AlertChannelType.WEBHOOK,
                target     : "http://example.com/default",
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

    def "save should update existing channel when id is provided"() {
        given: "an existing channel"
        def created = buildChannel(
                name: "channel-old",
                channelType: AlertChannelType.WEBHOOK,
                target: "http://example.com/old",
                enabled: true
        )
        alertChannelRepository.save(created)

        and: "existing row id"
        def existing = alertChannelRepository.findAll().first()

        when: "saving with same id and changed fields"
        def updated = buildChannel(
                id: existing.id,
                name: "channel-new",
                channelType: AlertChannelType.EMAIL,
                target: "mailto:new@example.com",
                enabled: false
        )
        alertChannelRepository.save(updated)

        and: "querying stored channels"
        def channels = alertChannelRepository.findAll()
        def persisted = alertChannelRepository.findById(existing.id)

        then: "it updates in-place instead of inserting a second row"
        channels.size() == 1
        persisted != null
        verifyAll(persisted) {
            id == existing.id
            name == "channel-new"
            channelType == AlertChannelType.EMAIL
            target == "mailto:new@example.com"
            enabled == false
        }
    }
}
