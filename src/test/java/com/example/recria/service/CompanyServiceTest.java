package com.example.recria.service;

import com.example.recria.dto.CompanyRequest;
import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.CompanySearchRequest;
import com.example.recria.entity.Company;
import com.example.recria.entity.Event;
import com.example.recria.repository.CompanyRepository;
import com.example.recria.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    private static final Long USER_ID = 1L;

    @Mock
    CompanyRepository companyRepository;

    @Mock
    EventRepository eventRepository;

    CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository, eventRepository);
    }

    static Event event(Long id, Long companyId, OffsetDateTime startAt, String tag) {
        Event event = new Event();
        event.setEventId(id);
        event.setCompanyId(companyId);
        event.setEventName("予定" + id);
        event.setStartAt(startAt);
        event.setEventTag(tag);
        return event;
    }

    static Company company(Long id, String name) {
        Company company = new Company();
        company.setCompanyId(id);
        company.setUserId(USER_ID);
        company.setCompanyName(name);
        company.setCompanyInfo("情報");
        company.setCompanyMemo("メモ");
        return company;
    }

    @Test
    void search_キーワードをそのままRepositoryに渡して結果を変換する() {
        CompanySearchRequest request = new CompanySearchRequest();
        request.setKeyword("テスト");
        when(companyRepository.findAll(USER_ID, "テスト")).thenReturn(List.of(company(10L, "テスト株式会社")));

        List<CompanyResponse> result = companyService.search(USER_ID, request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyId()).isEqualTo(10L);
        assertThat(result.get(0).getCompanyName()).isEqualTo("テスト株式会社");
        assertThat(result.get(0).getCompanyInfo()).isEqualTo("情報");
    }

    @Test
    void search_キーワード未指定なら全件検索になる() {
        when(companyRepository.findAll(USER_ID, null)).thenReturn(List.of(company(1L, "A社"), company(2L, "B社")));

        List<CompanyResponse> result = companyService.search(USER_ID, new CompanySearchRequest());

        assertThat(result).extracting(CompanyResponse::getCompanyName).containsExactly("A社", "B社");
        verify(companyRepository).findAll(USER_ID, null);
    }

    @Test
    void getDetail_企業と予定一覧を日本時間で返す() {
        when(companyRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(company(10L, "テスト株式会社")));
        OffsetDateTime utc = OffsetDateTime.of(2026, 12, 1, 1, 0, 0, 0, ZoneOffset.UTC);
        when(eventRepository.findByCompanyId(10L)).thenReturn(List.of(event(100L, 10L, utc, "面接")));

        CompanyResponse result = companyService.getDetail(USER_ID, 10L);

        assertThat(result.getCompanyName()).isEqualTo("テスト株式会社");
        assertThat(result.getEvents()).hasSize(1);
        assertThat(result.getEvents().get(0).getCompanyName()).isEqualTo("テスト株式会社");
        assertThat(result.getEvents().get(0).getStartAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(result.getEvents().get(0).getStartAt().getHour()).isEqualTo(10);
    }

    @Test
    void getDetail_他人の企業または存在しない企業は404() {
        when(companyRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getDetail(USER_ID, 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getReason()).isEqualTo("企業情報が見つかりません");
                });
        verify(eventRepository, never()).findByCompanyId(any());
    }

    @Test
    void hasDuplicateName_編集時は自分自身を除外し前後の空白を除いて判定する() {
        when(companyRepository.existsByUserIdAndCompanyName(USER_ID, "テスト株式会社", 10L)).thenReturn(true);

        assertThat(companyService.hasDuplicateName(USER_ID, "  テスト株式会社 ", 10L)).isTrue();
    }

    @Test
    void update_user_idを条件に含めて更新する() {
        CompanyRequest request = new CompanyRequest();
        request.setCompanyName(" 新社名 ");
        request.setCompanyInfo("新情報");
        when(companyRepository.update(any())).thenReturn(1);

        companyService.update(USER_ID, 10L, request);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).update(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getCompanyName()).isEqualTo("新社名");
        assertThat(captor.getValue().getCompanyInfo()).isEqualTo("新情報");
    }

    @Test
    void update_更新件数0なら404() {
        CompanyRequest request = new CompanyRequest();
        request.setCompanyName("新社名");
        when(companyRepository.update(any())).thenReturn(0);

        assertThatThrownBy(() -> companyService.update(USER_ID, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("企業情報が見つかりません");
    }

    @Test
    void getForEdit_現在の値を初期値にする() {
        when(companyRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(company(10L, "テスト株式会社")));

        CompanyRequest form = companyService.getForEdit(USER_ID, 10L);

        assertThat(form.getCompanyName()).isEqualTo("テスト株式会社");
        assertThat(form.getCompanyMemo()).isEqualTo("メモ");
    }
}
