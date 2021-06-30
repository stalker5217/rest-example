package com.example.restexample.configs;

import com.example.restexample.accounts.Account;
import com.example.restexample.accounts.AccountRepository;
import com.example.restexample.accounts.AccountRole;
import com.example.restexample.accounts.AccountService;
import com.example.restexample.common.BaseControllerTest;
import com.example.restexample.common.TestDescription;
import com.example.restexample.events.EventRepository;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthServerConfigTest extends BaseControllerTest {
    @Autowired
    AccountService accountService;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AppProperties appProperties;

    @Test
    @TestDescription("인증 토큰을 발급 받는 테스트")
    public void getAuthToken() throws Exception {
        this.mockMvc.perform(post("/oauth/token")
                    .with(httpBasic(appProperties.getClientId(), appProperties.getClientSecret()))
                    .param("username", appProperties.getUserUsername())
                    .param("password", appProperties.getUserPassword())
                    .param("grant_type", "password")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("access_token").exists())
        ;
    }
}