package com.bananice.businesstracer.presentation.http

import com.bananice.businesstracer.TestApplication
import com.bananice.businesstracer.application.alert.AlertConfigCacheService
import com.bananice.businesstracer.application.alert.AlertDispatchService
import com.bananice.businesstracer.domain.model.alert.AlertChannel
import com.bananice.businesstracer.domain.model.alert.AlertChannelType
import com.bananice.businesstracer.domain.model.alert.AlertRule
import com.bananice.businesstracer.domain.model.alert.AlertScopeType
import com.bananice.businesstracer.domain.model.alert.AlertStatus
import com.bananice.businesstracer.domain.model.alert.AlertType
import com.bananice.businesstracer.domain.repository.alert.AlertChannelRepository
import com.bananice.businesstracer.domain.repository.alert.AlertDispatchLogRepository
import com.bananice.businesstracer.domain.repository.alert.AlertEventRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.spockframework.spring.SpringBean
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = TestApplication)
@AutoConfigureMockMvc
class AlertControllerSpec extends Specification {

    @Autowired
    @Subject
    MockMvc mockMvc

    @SpringBean
    AlertConfigCacheService alertConfigCacheService = Mock()

    @SpringBean
    AlertChannelRepository alertChannelRepository = Mock()

    @SpringBean
    AlertEventRepository alertEventRepository = Mock()

    @SpringBean
    AlertDispatchLogRepository alertDispatchLogRepository = Mock()

    @Autowired
    AlertDispatchService alertDispatchService

    def "PUT NODE rule without flowCode returns 400"() {
        given:
        def requestBody = '''
{
  "name": "node rule",
  "alertType": "NODE_FAILED",
  "enabled": true
}
'''

        when:
        def result = mockMvc.perform(put("/business-tracer/api/alerts/rules/NODE/node-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(400))
                .andExpect(jsonPath('$.message').value('flowCode is required when scopeType is NODE'))

        and:
        0 * _
    }

    def "POST test-send dispatches selected channel even when disabled"() {
        given:
        def channel = AlertChannel.builder()
                .id(1001L)
                .name("webhook-main")
                .channelType(AlertChannelType.WEBHOOK)
                .target('{"url":"http://example.com/hook"}')
                .enabled(false)
                .build()

        when:
        def result = mockMvc.perform(post("/business-tracer/api/alerts/channels/1001/test-send")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{}'))

        then:
        1 * alertChannelRepository.findById(1001L) >> channel
        0 * alertChannelRepository.findEnabled()
        1 * alertDispatchLogRepository.save({ log ->
            log != null &&
                    log.channelId == 1001L &&
                    log.status == AlertStatus.SENT
        })

        and:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.message').value('test alert sent'))

        and:
        0 * _
    }

    def "GET rules returns existing rules from cache service"() {
        given:
        def globalRule = AlertRule.builder()
                .id(1L)
                .name("global-failed")
                .alertType(AlertType.NODE_FAILED)
                .scopeType(AlertScopeType.GLOBAL)
                .scopeRef("GLOBAL")
                .flowCode(null)
                .enabled(true)
                .build()

        def flowRule = AlertRule.builder()
                .id(2L)
                .name("flow-stuck")
                .alertType(AlertType.FLOW_STUCK)
                .scopeType(AlertScopeType.FLOW)
                .scopeRef("checkout")
                .flowCode("checkout")
                .enabled(true)
                .build()

        1 * alertConfigCacheService.listAllRules() >> [globalRule, flowRule]

        when:
        def result = mockMvc.perform(get("/business-tracer/api/alerts/rules"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.data[0].scopeType').value('GLOBAL'))
                .andExpect(jsonPath('$.data[1].scopeType').value('FLOW'))
    }

    def "PUT FLOW rule saves into cache service"() {
        given:
        def requestBody = '''
{
  "name": "flow failed",
  "alertType": "NODE_FAILED",
  "flowCode": "checkout",
  "enabled": true
}
'''

        when:
        def result = mockMvc.perform(put("/business-tracer/api/alerts/rules/FLOW/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.message').value('rule saved'))

        and:
        1 * alertConfigCacheService.saveRule({ rule ->
            rule != null &&
                    rule.name == 'flow failed' &&
                    rule.alertType == AlertType.NODE_FAILED &&
                    rule.scopeType == AlertScopeType.FLOW &&
                    rule.scopeRef == 'checkout' &&
                    rule.flowCode == 'checkout' &&
                    rule.enabled == true
        })
        0 * _
    }

    def "GET channels returns all channels"() {
        given:
        1 * alertChannelRepository.findAll() >> [
                AlertChannel.builder().id(10L).name('webhook').channelType(AlertChannelType.WEBHOOK).enabled(true).target('{}').build(),
                AlertChannel.builder().id(11L).name('email').channelType(AlertChannelType.EMAIL).enabled(false).target('{}').build()
        ]

        when:
        def result = mockMvc.perform(get('/business-tracer/api/alerts/channels'))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.data.length()').value(2))
                .andExpect(jsonPath('$.data[0].channelType').value('WEBHOOK'))
                .andExpect(jsonPath('$.data[1].channelType').value('EMAIL'))
    }

    def "POST channels creates channel"() {
        given:
        def requestBody = '''
{
  "name": "wecom-main",
  "channelType": "WECOM",
  "target": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=x",
  "enabled": true
}
'''

        when:
        def result = mockMvc.perform(post('/business-tracer/api/alerts/channels')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.message').value('channel created'))

        and:
        1 * alertChannelRepository.save({ channel ->
            channel != null &&
                    channel.id == null &&
                    channel.name == 'wecom-main' &&
                    channel.channelType == AlertChannelType.WECOM &&
                    channel.enabled == true
        })
        0 * _
    }

    def "PUT channels updates channel"() {
        given:
        def requestBody = '''
{
  "name": "dingtalk-main",
  "channelType": "DINGTALK",
  "target": "token",
  "enabled": false
}
'''

        when:
        def result = mockMvc.perform(put('/business-tracer/api/alerts/channels/321')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.message').value('channel updated'))

        and:
        1 * alertChannelRepository.save({ channel ->
            channel != null &&
                    channel.id == 321L &&
                    channel.name == 'dingtalk-main' &&
                    channel.channelType == AlertChannelType.DINGTALK &&
                    channel.enabled == false
        })
        0 * _
    }

    def "GET events returns paged results"() {
        given:
        def now = LocalDateTime.now()
        def event = com.bananice.businesstracer.domain.model.alert.AlertEvent.builder()
                .id(200L)
                .alertType(AlertType.SLOW_NODE)
                .status(AlertStatus.NEW)
                .flowCode('pay')
                .nodeCode('pay-node')
                .businessId('biz-200')
                .message('slow')
                .occurredAt(now)
                .build()

        1 * alertEventRepository.query(null, null, AlertType.SLOW_NODE, AlertStatus.NEW, 'pay', 'pay-node', 'biz-200', 1, 20) >> [event]
        1 * alertEventRepository.count(null, null, AlertType.SLOW_NODE, AlertStatus.NEW, 'pay', 'pay-node', 'biz-200') >> 1L

        when:
        def result = mockMvc.perform(get('/business-tracer/api/alerts/events')
                .param('alertType', 'SLOW_NODE')
                .param('status', 'NEW')
                .param('flowCode', 'pay')
                .param('nodeCode', 'pay-node')
                .param('businessId', 'biz-200')
                .param('pageNum', '1')
                .param('pageSize', '20'))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.data.total').value(1))
                .andExpect(jsonPath('$.data.list.length()').value(1))
                .andExpect(jsonPath('$.data.list[0].id').value(200))
    }

    def "GET event detail returns 404 when missing"() {
        given:
        alertEventRepository.findById(999L) >> null

        when:
        def result = mockMvc.perform(get('/business-tracer/api/alerts/events/999'))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(404))
                .andExpect(jsonPath('$.message').value('alert event not found: 999'))
    }

    def "GET event dispatch logs returns logs"() {
        given:
        alertDispatchLogRepository.findByEventId(200L) >> [
                com.bananice.businesstracer.domain.model.alert.AlertDispatchLog.builder()
                        .id(1L)
                        .eventId(200L)
                        .channelId(10L)
                        .status(AlertStatus.SENT)
                        .response('ok')
                        .dispatchTime(LocalDateTime.now())
                        .retryCount(0)
                        .build()
        ]

        when:
        def result = mockMvc.perform(get('/business-tracer/api/alerts/events/200/dispatch-logs'))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.code').value(200))
                .andExpect(jsonPath('$.data.length()').value(1))
                .andExpect(jsonPath('$.data[0].eventId').value(200))
    }
}
