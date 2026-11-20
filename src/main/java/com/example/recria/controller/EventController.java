package com.example.recria.controller;

import com.example.recria.dto.EventConflictResponse;
import com.example.recria.dto.EventRequest;
import com.example.recria.dto.EventResponse;
import com.example.recria.service.EventService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;

/** スケジュール・イベントに関するAPIを提供するController。 */
@RestController
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** GET /company/{companyId}/{eventId} : 予定詳細 */
    @GetMapping("/company/{companyId}/{eventId}")
    public EventResponse find(
            @PathVariable("companyId") Long companyId,
            @PathVariable("eventId") Long eventId,
            Principal principal) {
        return eventService.find(currentEmail(principal), companyId, eventId);
    }

    /** POST /company/{companyId}/event : 予定追加 */
    @PostMapping("/company/{companyId}/event")
    public ResponseEntity<EventResponse> create(
            @PathVariable("companyId") Long companyId,
            @Valid @RequestBody EventRequest request,
            Principal principal) {
        EventResponse response = eventService.create(currentEmail(principal), companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** POST /company/{companyId}/{eventId}/edit : 予定編集 */
    @PostMapping("/company/{companyId}/{eventId}/edit")
    public EventResponse update(
            @PathVariable("companyId") Long companyId,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody EventRequest request,
            Principal principal) {
        return eventService.update(currentEmail(principal), companyId, eventId, request);
    }

    /** POST /company/{companyId}/{eventId}/delete : 予定削除 */
    @PostMapping("/company/{companyId}/{eventId}/delete")
    public ResponseEntity<Void> delete(
            @PathVariable("companyId") Long companyId,
            @PathVariable("eventId") Long eventId,
            Principal principal) {
        eventService.delete(currentEmail(principal), companyId, eventId);
        return ResponseEntity.noContent().build();
    }

    /** GET /events?from=&to= : 指定期間内の予定一覧（カレンダー用） */
    @GetMapping("/events")
    public List<EventResponse> list(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Principal principal) {
        return eventService.list(currentEmail(principal), from, to);
    }

    /** GET /events/conflicts?startAt=&endAt= : 予定の重複確認 */
    @GetMapping("/events/conflicts")
    public EventConflictResponse checkOverlap(
            @RequestParam("startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam(value = "endAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt,
            @RequestParam(value = "excludeEventId", required = false) Long excludeEventId,
            Principal principal) {
        return eventService.checkOverlap(currentEmail(principal), startAt, endAt, excludeEventId);
    }

    private String currentEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください");
        }
        return principal.getName();
    }
}

