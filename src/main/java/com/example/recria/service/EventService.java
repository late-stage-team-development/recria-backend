package com.example.recria.service;

import com.example.recria.dto.EventConflictResponse;
import com.example.recria.dto.EventRequest;
import com.example.recria.dto.EventResponse;
import com.example.recria.entity.Company;
import com.example.recria.entity.Event;
import com.example.recria.entity.User;
import com.example.recria.repository.CompanyRepository;
import com.example.recria.repository.EventRepository;
import com.example.recria.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

/** スケジュール・イベントの取得、登録、更新、削除、重複確認を担当するService。 */
@Service
public class EventService {

    private final CompanyRepository companyRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(
            CompanyRepository companyRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /** 予定の詳細を取得する。 */
    public EventResponse find(String email, Long companyId, Long eventId) {
        Long userId = resolveUserId(email);
        Company company = requireOwnedCompany(companyId, userId);
        Event event = requireEvent(companyId, eventId);
        return toResponse(event, company);
    }

    /** 指定期間内のログインユーザー本人の予定を取得する。 */
    public List<EventResponse> list(String email, OffsetDateTime from, OffsetDateTime to) {
        Long userId = resolveUserId(email);
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "表示期間の開始日時と終了日時を正しく指定してください");
        }

        return eventRepository.findByPeriod(from, to, userId).stream()
                .map(event -> {
                    Company company = companyRepository.findById(event.getCompanyId())
                            .orElseThrow(() -> notFound("企業情報が見つかりません"));
                    return toResponse(event, company);
                })
                .toList();
    }

    /** 予定の重複を確認する。重複する予定はすべて返す。 */
    public EventConflictResponse checkOverlap(
            String email, OffsetDateTime startAt, OffsetDateTime endAt, Long excludeEventId) {
        Long userId = resolveUserId(email);
        validateScheduleTime(startAt, endAt);

        List<EventResponse> conflicts = eventRepository
                .findOverlaps(userId, startAt, endAt, excludeEventId)
                .stream()
                .map(event -> {
                    Company company = companyRepository.findById(event.getCompanyId())
                            .orElseThrow(() -> notFound("企業情報が見つかりません"));
                    return toResponse(event, company);
                })
                .toList();

        boolean hasConflict = !conflicts.isEmpty();
        String message = hasConflict
                ? "既存の予定と重複しています"
                : "重複する予定はありません";
        return new EventConflictResponse(hasConflict, message, conflicts);
    }

    /** 企業に予定を登録する。 */
    @Transactional
    public EventResponse create(String email, Long companyId, EventRequest request) {
        Long userId = resolveUserId(email);
        Company company = requireOwnedCompany(companyId, userId);
        validateScheduleTime(request.getStartAt(), request.getEndAt());
        validateNotifyAt(request.getNotifyAt(), request.getStartAt());

        Event event = new Event();
        event.setCompanyId(companyId);
        applyRequest(event, request);
        event.setResult(null);

        Long eventId = eventRepository.create(event);
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "予定を登録できませんでした");
        }

        Event saved = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "登録した予定を取得できませんでした"));
        return toResponse(saved, company);
    }

    /** 予定を更新する。 */
    @Transactional
    public EventResponse update(String email, Long companyId, Long eventId, EventRequest request) {
        Long userId = resolveUserId(email);
        Company company = requireOwnedCompany(companyId, userId);
        Event event = requireEvent(companyId, eventId);
        validateScheduleTime(request.getStartAt(), request.getEndAt());
        validateNotifyAt(request.getNotifyAt(), request.getStartAt());

        // EventRequestには合否項目がないため、既存のresultは維持する。
        String existingResult = event.getResult();
        applyRequest(event, request);
        event.setResult(existingResult);

        if (eventRepository.update(event) == 0) {
            throw notFound("予定情報が見つかりません");
        }
        Event updated = eventRepository.findById(eventId)
                .orElseThrow(() -> notFound("予定情報が見つかりません"));
        return toResponse(updated, company);
    }

    /** 予定を削除する。 */
    @Transactional
    public void delete(String email, Long companyId, Long eventId) {
        Long userId = resolveUserId(email);
        requireOwnedCompany(companyId, userId);
        requireEvent(companyId, eventId);
        if (eventRepository.delete(eventId, companyId) == 0) {
            throw notFound("予定情報が見つかりません");
        }
    }

    /** 認証済みユーザーのメールアドレスからユーザーIDを取得する。 */
    public Long resolveUserId(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください");
        }
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "ログインしてください"));
    }

    /** 指定企業がログインユーザー本人の所有物であることを確認する。 */
    private Company requireOwnedCompany(Long companyId, Long userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> notFound("企業情報が見つかりません"));
        if (!userId.equals(company.getUserId())) {
            // 他人のデータの存在を推測できないよう、存在しない場合と同じ404にする。
            throw notFound("企業情報が見つかりません");
        }
        return company;
    }

    /** 指定企業に紐づく予定であることを確認する。 */
    private Event requireEvent(Long companyId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> notFound("予定情報が見つかりません"));
        if (!companyId.equals(event.getCompanyId())) {
            throw notFound("予定情報が見つかりません");
        }
        return event;
    }

    private void validateScheduleTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || !startAt.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "過去の日付は登録できません");
        }
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "終了日時は開始日時以降に設定してください");
        }
    }

    private void validateNotifyAt(OffsetDateTime notifyAt, OffsetDateTime startAt) {
        if (notifyAt == null) return;
        if (!notifyAt.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "通知日時は現在より未来に設定してください");
        }
        if (startAt != null && !notifyAt.isBefore(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "通知日時は予定日時より前に設定してください");
        }
    }

    private void applyRequest(Event event, EventRequest request) {
        event.setEventName(request.getEventName().trim());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setNotifyAt(request.getNotifyAt());
        event.setEventMemo(request.getEventMemo());
        event.setEventTag(request.getEventTag());
    }

    private EventResponse toResponse(Event event, Company company) {
        EventResponse response = new EventResponse();
        response.setEventId(event.getEventId());
        response.setCompanyId(event.getCompanyId());
        response.setCompanyName(company.getCompanyName());
        response.setEventName(event.getEventName());
        response.setStartAt(event.getStartAt());
        response.setEndAt(event.getEndAt());
        response.setNotifyAt(event.getNotifyAt());
        response.setEventMemo(event.getEventMemo());
        response.setEventTag(event.getEventTag());
        response.setResult(event.getResult());
        return response;
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
