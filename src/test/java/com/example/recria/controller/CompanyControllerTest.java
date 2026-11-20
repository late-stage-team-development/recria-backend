package com.example.recria.controller;

import com.example.recria.dto.CompanyResponse;
import com.example.recria.entity.User;
import com.example.recria.repository.UserRepository;
import com.example.recria.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {
    static final Long USER_ID = 1L;
    static final String EMAIL = "user@example.com";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CompanyService companyService;

    @MockitoBean
    UserRepository userRepository;

    // RecriaApplication の接続確認用 CommandLineRunner が要求するため
    @MockitoBean
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    static CompanyResponse company(Long id, String name) {
        CompanyResponse response = new CompanyResponse();
        response.setCompanyId(id);
        response.setCompanyName(name);
        return response;
    }

    @Test
    void list_企業一覧を表示する() throws Exception {
        when(companyService.search(eq(USER_ID), any())).thenReturn(List.of(company(5L, "テスト株式会社")));

        mockMvc.perform(get("/company").with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("company/list"))
                .andExpect(content().string(containsString("テスト株式会社")))
                .andExpect(content().string(containsString("/company/5")));
    }

    @Test
    void list_検索結果0件なら該当なしメッセージを表示する() throws Exception {
        when(companyService.search(eq(USER_ID), any())).thenReturn(List.of());

        mockMvc.perform(get("/company").param("keyword", "存在しない").with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("該当する企業がありません")));
    }

    @Test
    void list_キーワード100文字超はエラーを表示し検索しない() throws Exception {
        mockMvc.perform(get("/company").param("keyword", "あ".repeat(101)).with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("search", "keyword"))
                .andExpect(content().string(containsString("検索キーワードは100文字以内で入力して下さい")))
                .andExpect(content().string(not(containsString("該当する企業がありません"))));
        verify(companyService, never()).search(any(), any());
    }

    @Test
    void list_想定外エラーは共通エラー画面を表示する() throws Exception {
        when(companyService.search(eq(USER_ID), any())).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/company").with(user(EMAIL)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("エラーが発生しました。時間をおいて再度お試しください")));
    }
}
