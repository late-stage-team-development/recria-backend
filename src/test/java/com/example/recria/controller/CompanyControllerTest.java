package com.example.recria.controller;

import com.example.recria.dto.CompanyRequest;
import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.EventResponse;
import com.example.recria.entity.User;
import com.example.recria.repository.UserRepository;
import com.example.recria.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    @Test
    void detail_企業と予定一覧を表示する() throws Exception {
        CompanyResponse company = company(5L, "テスト株式会社");
        company.setCompanyInfo("IT企業");
        EventResponse event = new EventResponse();
        event.setEventId(50L);
        event.setCompanyId(5L);
        event.setEventName("一次面接");
        event.setEventTag("面接");
        event.setStartAt(OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9)));
        company.setEvents(List.of(event));
        when(companyService.getDetail(USER_ID, 5L)).thenReturn(company);

        mockMvc.perform(get("/company/5").with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("company/detail"))
                .andExpect(content().string(containsString("IT企業")))
                .andExpect(content().string(containsString("一次面接")))
                .andExpect(content().string(containsString("2026/12/01")))
                .andExpect(content().string(containsString("/company/5/50/reflection")));
    }

    @Test
    void detail_他人の企業や存在しない企業は404で企業情報が見つかりませんを表示する() throws Exception {
        when(companyService.getDetail(USER_ID, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "企業情報が見つかりません"));

        mockMvc.perform(get("/company/99").with(user(EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("企業情報が見つかりません")));
    }

    @Test
    void detail_数値でないIDも404() throws Exception {
        mockMvc.perform(get("/company/abc").with(user(EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("企業情報が見つかりません")));
    }

    @Test
    void editForm_現在の値を表示する() throws Exception {
        CompanyRequest form = new CompanyRequest();
        form.setCompanyName("テスト株式会社");
        when(companyService.getForEdit(USER_ID, 5L)).thenReturn(form);

        mockMvc.perform(get("/company/5/edit").with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("company/edit"))
                .andExpect(content().string(containsString("value=\"テスト株式会社\"")));
    }

    @Test
    void edit_成功したら詳細へリダイレクトし完了メッセージを渡す() throws Exception {
        when(companyService.hasDuplicateName(USER_ID, "新社名", 5L)).thenReturn(false);

        mockMvc.perform(post("/company/5/edit").with(user(EMAIL)).with(csrf())
                        .param("companyName", "新社名").param("companyInfo", "情報"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/company/5"))
                .andExpect(flash().attribute("message", "企業情報を更新しました"));
        verify(companyService).update(eq(USER_ID), eq(5L), argThat(f -> "新社名".equals(f.getCompanyName())));
    }

    @Test
    void edit_入力エラーは項目ごとのメッセージを表示する() throws Exception {
        mockMvc.perform(post("/company/5/edit").with(user(EMAIL)).with(csrf())
                        .param("companyName", "").param("companyInfo", "あ".repeat(1001)))
                .andExpect(status().isOk())
                .andExpect(view().name("company/edit"))
                .andExpect(content().string(containsString("会社名を入力して下さい")))
                .andExpect(content().string(containsString("会社情報は1000文字以内で入力して下さい")));
        verify(companyService, never()).update(any(), any(), any());
    }

    @Test
    void edit_同名企業があれば警告し保存しない() throws Exception {
        when(companyService.hasDuplicateName(USER_ID, "既存社名", 5L)).thenReturn(true);

        mockMvc.perform(post("/company/5/edit").with(user(EMAIL)).with(csrf()).param("companyName", "既存社名"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("duplicateWarning", true))
                .andExpect(content().string(containsString("同じ名前の企業がすでに登録されています")))
                .andExpect(content().string(containsString("それでも保存する")));
        verify(companyService, never()).update(any(), any(), any());
    }

    @Test
    void edit_警告後にそれでも保存するを選ぶと保存する() throws Exception {
        mockMvc.perform(post("/company/5/edit").with(user(EMAIL)).with(csrf())
                        .param("companyName", "既存社名").param("confirmDuplicate", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/company/5"));
        verify(companyService, never()).hasDuplicateName(any(), any(), any());
        verify(companyService).update(eq(USER_ID), eq(5L), any());
    }

    @Test
    void edit_他人の企業は404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "企業情報が見つかりません"))
                .when(companyService).checkOwner(USER_ID, 99L);

        mockMvc.perform(post("/company/99/edit").with(user(EMAIL)).with(csrf()).param("companyName", "社名"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("企業情報が見つかりません")));
        verify(companyService, never()).update(any(), any(), any());
    }
}
