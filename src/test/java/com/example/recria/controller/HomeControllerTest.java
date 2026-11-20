package com.example.recria.controller;

import com.example.recria.dto.EventResponse;
import com.example.recria.dto.HomeResponse;
import com.example.recria.entity.User;
import com.example.recria.repository.UserRepository;
import com.example.recria.service.HomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    static final Long USER_ID = 1L;
    static final String EMAIL = "user@example.com";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HomeService homeService;

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

    static HomeResponse home(List<EventResponse> upcoming, List<EventResponse> notifications, List<EventResponse> unreflected) {
        HomeResponse home = new HomeResponse();
        home.setUpcomingEvents(upcoming);
        home.setNotifications(notifications);
        home.setUnreflectedEvents(unreflected);
        return home;
    }

    static EventResponse event(Long eventId, Long companyId, String companyName, String eventName) {
        EventResponse event = new EventResponse();
        event.setEventId(eventId);
        event.setCompanyId(companyId);
        event.setCompanyName(companyName);
        event.setEventName(eventName);
        event.setEventTag("面接");
        event.setStartAt(OffsetDateTime.of(2026, 11, 25, 10, 0, 0, 0, ZoneOffset.ofHours(9)));
        return event;
    }

    @Test
    void 予定が無ければ直近2週間の予定はありませんを表示しお知らせは出さない() throws Exception {
        when(homeService.getHome(USER_ID)).thenReturn(home(List.of(), List.of(), List.of()));

        mockMvc.perform(get("/home").with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("直近2週間の予定はありません")))
                .andExpect(content().string(not(containsString("の振り返りを書きましょう"))))
                .andExpect(content().string(not(containsString("が近づいています"))));
    }

    @Test
    void 予定は企業詳細へお知らせは振り返り画面へリンクする() throws Exception {
        when(homeService.getHome(USER_ID)).thenReturn(home(
                List.of(event(1L, 10L, "A社", "一次面接")),
                List.of(event(2L, 20L, "B社", "最終面接")),
                List.of(event(3L, 30L, "C社", "二次面接"))));

        mockMvc.perform(get("/home").with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A社：一次面接")))
                .andExpect(content().string(containsString("href=\"/company/10\"")))
                .andExpect(content().string(containsString("11/25")))
                .andExpect(content().string(containsString("B社：最終面接")))
                .andExpect(content().string(containsString("が近づいています")))
                .andExpect(content().string(containsString("C社：二次面接")))
                .andExpect(content().string(containsString("href=\"/company/30/3/reflection\"")))
                .andExpect(content().string(not(containsString("直近2週間の予定はありません"))));
    }

    @Test
    void 想定外エラーは共通エラー画面を表示する() throws Exception {
        when(homeService.getHome(USER_ID)).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/home").with(user(EMAIL)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("エラーが発生しました。時間をおいて再度お試しください")));
    }
}
