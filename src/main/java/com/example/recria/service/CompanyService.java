package com.example.recria.service;

import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.CompanySearchRequest;
import com.example.recria.dto.EventResponse;
import com.example.recria.entity.Company;
import com.example.recria.entity.Event;
import com.example.recria.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** 企業の検索・登録・更新・削除を行うService。 */
@Service
public class CompanyService {
    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    public static final String COMPANY_NOT_FOUND = "企業情報が見つかりません";

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /** ログインユーザーの企業を企業名で検索する。キーワードが空なら全件を返す。 */
    public List<CompanyResponse> search(Long userId, CompanySearchRequest request) {
        String keyword = request == null ? null : request.getKeyword();
        return companyRepository.findAll(userId, keyword).stream()
                .map(company -> toCompanyResponse(company, null))
                .toList();
    }

    static CompanyResponse toCompanyResponse(Company company, List<EventResponse> events) {
        CompanyResponse response = new CompanyResponse();
        response.setCompanyId(company.getCompanyId());
        response.setCompanyName(company.getCompanyName());
        response.setCompanyInfo(company.getCompanyInfo());
        response.setCompanyMemo(company.getCompanyMemo());
        response.setEvents(events);
        return response;
    }

    /** 予定を画面表示用に変換する。日時は日本時間にそろえる。 */
    static EventResponse toEventResponse(Event event, String companyName) {
        EventResponse response = new EventResponse();
        response.setEventId(event.getEventId());
        response.setCompanyId(event.getCompanyId());
        response.setCompanyName(companyName);
        response.setEventName(event.getEventName());
        response.setStartAt(toTokyo(event.getStartAt()));
        response.setEndAt(toTokyo(event.getEndAt()));
        response.setNotifyAt(toTokyo(event.getNotifyAt()));
        response.setEventMemo(event.getEventMemo());
        response.setEventTag(event.getEventTag());
        response.setResult(event.getResult());
        return response;
    }

    static OffsetDateTime toTokyo(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(TOKYO).toOffsetDateTime();
    }
}
