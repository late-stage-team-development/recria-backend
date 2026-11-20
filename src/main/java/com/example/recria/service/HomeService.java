package com.example.recria.service;

import com.example.recria.dto.EventResponse;
import com.example.recria.dto.HomeResponse;
import com.example.recria.entity.Company;
import com.example.recria.entity.Event;
import com.example.recria.repository.CompanyRepository;
import com.example.recria.repository.EventRepository;
import com.example.recria.repository.ReflectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** ホーム画面に表示する予定・通知・お知らせを集めるService。 */
@Service
public class HomeService {
    static final String INTERVIEW_TAG = "面接";
    static final int UPCOMING_DAYS = 14;

    private final CompanyRepository companyRepository;
    private final EventRepository eventRepository;
    private final ReflectionRepository reflectionRepository;
    private final Clock clock;

    @Autowired
    public HomeService(CompanyRepository companyRepository, EventRepository eventRepository,
                       ReflectionRepository reflectionRepository) {
        this(companyRepository, eventRepository, reflectionRepository, Clock.system(CompanyService.TOKYO));
    }

    HomeService(CompanyRepository companyRepository, EventRepository eventRepository,
                ReflectionRepository reflectionRepository, Clock clock) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.reflectionRepository = reflectionRepository;
        this.clock = clock;
    }

    public HomeResponse getHome(Long userId) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(CompanyService.TOKYO));
        OffsetDateTime nowOffset = now.toOffsetDateTime();
        OffsetDateTime todayStart = now.toLocalDate().atStartOfDay(CompanyService.TOKYO).toOffsetDateTime();

        List<Company> companies = companyRepository.findAll(userId);
        Map<Long, String> companyNames = companies.stream()
                .collect(Collectors.toMap(Company::getCompanyId, Company::getCompanyName));

        HomeResponse response = new HomeResponse();
        response.setUpcomingEvents(toResponses(upcomingEvents(userId, todayStart), companyNames));

        // 通知・未振り返りは期間の上限/下限がないため、企業ごとに予定を取得して抽出する
        List<Event> allEvents = companies.stream()
                .flatMap(company -> eventRepository.findByCompanyId(company.getCompanyId()).stream())
                .toList();
        response.setNotifications(toResponses(notifications(allEvents, nowOffset), companyNames));
        response.setUnreflectedEvents(toResponses(unreflectedInterviews(allEvents, nowOffset), companyNames));
        return response;
    }

    /** 当日0:00〜14日後0:00（日本時間）に始まる予定。開始日時の早い順。 */
    private List<Event> upcomingEvents(Long userId, OffsetDateTime todayStart) {
        // findByPeriod は期間と重なる予定（前日開始で当日終了など）も返すため、開始日時で絞り込む
        return eventRepository.findByPeriod(todayStart, todayStart.plusDays(UPCOMING_DAYS), userId).stream()
                .filter(event -> !event.getStartAt().isBefore(todayStart))
                .sorted(Comparator.comparing(Event::getStartAt))
                .toList();
    }

    /** 通知日時を過ぎていて、まだ始まっていない予定。開始日時の早い順。 */
    private List<Event> notifications(List<Event> events, OffsetDateTime now) {
        return events.stream()
                .filter(event -> event.getNotifyAt() != null && !event.getNotifyAt().isAfter(now))
                .filter(event -> event.getStartAt().isAfter(now))
                .sorted(Comparator.comparing(Event::getStartAt))
                .toList();
    }

    /** 開始日時を過ぎた「面接」のうち、振り返りがまだ無いもの。開始日時の早い順。 */
    private List<Event> unreflectedInterviews(List<Event> events, OffsetDateTime now) {
        return events.stream()
                .filter(event -> INTERVIEW_TAG.equals(event.getEventTag()))
                .filter(event -> event.getStartAt().isBefore(now))
                .filter(event -> reflectionRepository.findByEventId(event.getEventId()).isEmpty())
                .sorted(Comparator.comparing(Event::getStartAt))
                .toList();
    }

    private static List<EventResponse> toResponses(List<Event> events, Map<Long, String> companyNames) {
        return events.stream()
                .map(event -> CompanyService.toEventResponse(event, companyNames.get(event.getCompanyId())))
                .toList();
    }
}
