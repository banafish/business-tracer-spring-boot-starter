package com.bananice.businesstracer.presentation.http

import com.bananice.businesstracer.TestApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = TestApplication)
@AutoConfigureMockMvc
class AlertFrontendResourceSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    def "GET alerts page resource exists"() {
        expect:
        mockMvc.perform(get('/business-tracer/alerts.html'))
                .andExpect(status().isOk())
    }

    def "GET alerts script resource exists"() {
        expect:
        mockMvc.perform(get('/business-tracer/alerts.js'))
                .andExpect(status().isOk())
    }

    def "sidebar contains alert center nav"() {
        expect:
        mockMvc.perform(get('/business-tracer/sidebar.js'))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString('/business-tracer/alerts.html')))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id: 'alerts'")))
    }
}
