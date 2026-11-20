package com.example.recria.service;

import com.example.recria.dto.EventResponse;
import com.example.recria.dto.HomeResponse;
import com.example.recria.entity.Company;
import com.example.recria.entity.Event;
import com.example.recria.entity.Reflection;
import com.example.recria.repository.CompanyRepository;
import com.example.recria.repository.EventRepository;
import com.example.recria.repository.ReflectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeServiceTest {
    static final Long USER_ID = 1L;
    static final ZoneOffset JST = ZoneOffset.ofHours(9);
    /** 現在時刻：2026/11/20 12:00（日本時間） */
    static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 20, 12, 0, 0, 0, JST);
    static final OffsetDateTime TODAY = OffsetDateTime.of(2026, 11, 20, 0, 0, 0, 0, JST);

    @Mock
    CompanyRepository companyRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    ReflectionRepository reflectionRepository;

    HomeService homeService;

    @BeforeEach
    void setUp() {
        // サーバーのタイムゾーンがUTCでも日本時間で「当日」を判定することを確かめるため、UTCの時計を渡す
        Clock clock = Clock.fixed(NOW.toInstant(), ZoneId.of("UTC"));
        homeService = new HomeService(companyRepository, eventRepository, reflectionRepository, clock);
        Company company = new Company();
        company.setCompanyId(10L);
        company.setCompanyName("A社");
        when(companyRepository.findAll(USER_ID)).thenReturn(List.of(company));
        when(eventRepository.findByCompanyId(10L)).thenReturn(List.of());
        when(eventRepository.findByPeriod(any(), any(), any())).thenReturn(List.of());
        when(reflectionRepository.findByEventId(anyLong())).thenReturn(Optional.empty());
    }

    static Event event(Long id, OffsetDateTime startAt, String tag, OffsetDateTime notifyAt) {
        Event event = new Event();
        event.setEventId(id);
        event.setCompanyId(10L);
        event.setEventName("予定" + id);
        event.setStartAt(startAt);
        event.setEventTag(tag);
        event.setNotifyAt(notifyAt);
        return event;
    }

    @Test
    void upcoming_当日0時から14日後0時までを日本時間で取得し開始日時順に並べる() {
        Event yesterdayStart = event(1L, TODAY.minusHours(2), null, null);
        Event later = event(2L, TODAY.plusDays(13).plusHours(23), null, null);
        Event todayMorning = event(3L, TODAY.plusHours(9), null, null);
        when(eventRepository.findByPeriod(TODAY, TODAY.plusDays(14), USER_ID))
                .thenReturn(List.of(yesterdayStart, later, todayMorning));

        HomeResponse home = homeService.getHome(USER_ID);

        verify(eventRepository).findByPeriod(TODAY, TODAY.plusDays(14), USER_ID);
        assertThat(home.getUpcomingEvents()).extracting(EventResponse::getEventId).containsExactly(3L, 2L);
        assertThat(home.getUpcomingEvents().get(0).getCompanyName()).isEqualTo("A社");
    }

    @Test
    void notifications_通知日時を過ぎて開始前の予定だけを返す() {
        Event target = event(1L, NOW.plusDays(3), null, NOW.minusHours(1));
        Event notifyJustNow = event(2L, NOW.plusDays(1), null, NOW);
        Event notYet = event(3L, NOW.plusDays(5), null, NOW.plusDays(2));
        Event alreadyStarted = event(4L, NOW.minusHours(1), null, NOW.minusDays(3));
        Event noNotify = event(5L, NOW.plusDays(1), null, null);
        when(eventRepository.findByCompanyId(10L))
                .thenReturn(List.of(target, notifyJustNow, notYet, alreadyStarted, noNotify));

        HomeResponse home = homeService.getHome(USER_ID);

        assertThat(home.getNotifications()).extracting(EventResponse::getEventId).containsExactly(2L, 1L);
    }

    @Test
    void unreflected_開始を過ぎた面接で振り返りが無いものだけを返す() {
        Event target = event(1L, NOW.minusDays(1), "面接", null);
        Event reflected = event(2L, NOW.minusDays(2), "面接", null);
        Event otherTag = event(3L, NOW.minusDays(1), "説明会", null);
        Event future = event(4L, NOW.plusDays(1), "面接", null);
        when(eventRepository.findByCompanyId(10L)).thenReturn(List.of(target, reflected, otherTag, future));
        when(reflectionRepository.findByEventId(2L)).thenReturn(Optional.of(new Reflection()));

        HomeResponse home = homeService.getHome(USER_ID);

        assertThat(home.getUnreflectedEvents()).extracting(EventResponse::getEventId).containsExactly(1L);
    }

    @Test
    void 予定が無ければすべて空リスト() {
        HomeResponse home = homeService.getHome(USER_ID);

        assertThat(home.getUpcomingEvents()).isEmpty();
        assertThat(home.getNotifications()).isEmpty();
        assertThat(home.getUnreflectedEvents()).isEmpty();
    }
}
