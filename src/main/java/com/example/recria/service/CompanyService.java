package com.example.recria.service;

import com.example.recria.dto.CompanyRegisterRequest;
import com.example.recria.dto.CompanyRequest;
import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.CompanySearchRequest;
import com.example.recria.dto.EventResponse;
import com.example.recria.entity.Company;
import com.example.recria.entity.Event;
import com.example.recria.repository.CompanyRepository;
import com.example.recria.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 企業の検索・登録・更新・削除を行うService。 */
@Service
public class CompanyService {
    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    public static final String COMPANY_NOT_FOUND = "企業情報が見つかりません";

    private final CompanyRepository companyRepository;
    private final EventRepository eventRepository;

    public CompanyService(CompanyRepository companyRepository, EventRepository eventRepository) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
    }

    /** ログインユーザーの企業を企業名で検索する。キーワードが空なら全件を返す。 */
    public List<CompanyResponse> search(Long userId, CompanySearchRequest request) {
        String keyword = request == null ? null : request.getKeyword();
        return companyRepository.findAll(userId, keyword).stream()
                .map(company -> toCompanyResponse(company, null))
                .toList();
    }

    /** 企業詳細を紐づく予定一覧つきで取得する。本人以外・存在しない企業は404。 */
    public CompanyResponse getDetail(Long userId, Long companyId) {
        Company company = findOwnCompany(userId, companyId);
        List<EventResponse> events = eventRepository.findByCompanyId(company.getCompanyId()).stream()
                .map(event -> toEventResponse(event, company.getCompanyName()))
                .toList();
        return toCompanyResponse(company, events);
    }

    /** 編集画面の初期値を取得する。本人以外・存在しない企業は404。 */
    public CompanyRequest getForEdit(Long userId, Long companyId) {
        Company company = findOwnCompany(userId, companyId);
        CompanyRequest request = new CompanyRequest();
        request.setCompanyName(company.getCompanyName());
        request.setCompanyInfo(company.getCompanyInfo());
        request.setCompanyMemo(company.getCompanyMemo());
        return request;
    }

    /** 本人の企業であることを確認する。本人以外・存在しない企業は404。 */
    public void checkOwner(Long userId, Long companyId) {
        findOwnCompany(userId, companyId);
    }

    /**
     * 同じユーザー内に同名の企業があるか判定する（VAL-03）。
     * 編集時は excludeCompanyId に自分自身のIDを渡す。登録時は null。
     */
    public boolean hasDuplicateName(Long userId, String companyName, Long excludeCompanyId) {
        return companyRepository.existsByUserIdAndCompanyName(userId, normalize(companyName), excludeCompanyId);
    }

    /** 企業情報を更新する。本人以外・存在しない企業は404。 */
    public void update(Long userId, Long companyId, CompanyRequest request) {
        Company company = new Company();
        company.setCompanyId(companyId);
        company.setUserId(userId);
        company.setCompanyName(normalize(request.getCompanyName()));
        company.setCompanyInfo(request.getCompanyInfo());
        company.setCompanyMemo(request.getCompanyMemo());
        if (companyRepository.update(company) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMPANY_NOT_FOUND);
        }
    }

    /** 指定した時間帯と重なる自分の予定を取得する（FR-06）。 */
    public List<EventResponse> findOverlaps(Long userId, OffsetDateTime startAt, OffsetDateTime endAt) {
        List<Event> overlaps = eventRepository.findOverlaps(userId, startAt, endAt, null);
        if (overlaps.isEmpty()) {
            return List.of();
        }
        Map<Long, String> companyNames = companyRepository.findAll(userId).stream()
                .collect(Collectors.toMap(Company::getCompanyId, Company::getCompanyName));
        return overlaps.stream()
                .map(event -> toEventResponse(event, companyNames.get(event.getCompanyId())))
                .toList();
    }

    /** 企業と最初の予定を1トランザクションで登録し、企業IDを返す（API-04）。 */
    @Transactional
    public Long register(Long userId, CompanyRegisterRequest request) {
        Company company = new Company();
        company.setUserId(userId);
        company.setCompanyName(normalize(request.getCompanyName()));
        company.setCompanyInfo(request.getCompanyInfo());
        company.setCompanyMemo(request.getCompanyMemo());
        Long companyId = companyRepository.create(company);

        Event event = new Event();
        event.setCompanyId(companyId);
        event.setEventName(request.getEventName());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setNotifyAt(request.getNotifyAt());
        event.setEventMemo(request.getEventMemo());
        event.setEventTag(emptyToNull(request.getEventTag()));
        eventRepository.create(event);
        return companyId;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** 企業名の前後の空白を取り除く（同名判定と保存で同じ値を使うため）。 */
    static String normalize(String companyName) {
        return companyName == null ? null : companyName.strip();
    }

    /** ログインユーザー本人の企業を取得する。見つからなければ404とする。 */
    Company findOwnCompany(Long userId, Long companyId) {
        return companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMPANY_NOT_FOUND));
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
