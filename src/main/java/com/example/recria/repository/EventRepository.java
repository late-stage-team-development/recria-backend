package com.example.recria.repository;

import com.example.recria.entity.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** eventsテーブルを操作するRepository。 */
@Repository
public class EventRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Event> rowMapper = (rs, rowNum) -> {
        Event event = new Event();
        event.setEventId(rs.getLong("event_id"));
        event.setCompanyId(rs.getLong("company_id"));
        event.setEventName(rs.getString("event_name"));
        event.setStartAt(rs.getObject("start_at", OffsetDateTime.class));
        event.setEndAt(rs.getObject("end_at", OffsetDateTime.class));
        event.setEventTag(rs.getString("event_tag"));
        event.setEventMemo(rs.getString("event_memo"));
        event.setNotifyAt(rs.getObject("notify_at", OffsetDateTime.class));
        event.setResult(rs.getString("result"));
        event.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return event;
    };

    public EventRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 企業IDに紐づく予定一覧を取得する。 */
    public List<Event> findByCompanyId(Long companyId) {
        return jdbcTemplate.query("SELECT event_id, company_id, event_name, start_at, end_at, event_tag, event_memo, " +
                "notify_at, result, created_at FROM events WHERE company_id = ? ORDER BY start_at", rowMapper, companyId);
    }

    /** 予定IDから予定を取得する。 */
    public Optional<Event> findById(Long eventId) {
        return jdbcTemplate.query("SELECT event_id, company_id, event_name, start_at, end_at, event_tag, event_memo, " +
                "notify_at, result, created_at FROM events WHERE event_id = ?", rowMapper, eventId).stream().findFirst();
    }

    /** 指定期間と重なる予定を取得する（ログインユーザーの予定のみ）。 */
    public List<Event> findByPeriod(OffsetDateTime from, OffsetDateTime to, Long userId) {
        return jdbcTemplate.query("SELECT e.event_id, e.company_id, e.event_name, e.start_at, e.end_at, e.event_tag, " +
                        "e.event_memo, e.notify_at, e.result, e.created_at FROM events e " +
                        "JOIN companies c ON c.company_id = e.company_id " +
                        "WHERE c.user_id = ? AND e.start_at < ? AND COALESCE(e.end_at, e.start_at) >= ? ORDER BY e.start_at",
                rowMapper, userId, Timestamp.from(to.toInstant()), Timestamp.from(from.toInstant()));
    }

    /** 指定時間帯と重複する予定を取得する。更新時は除外するeventIdを指定できる。 */
    public List<Event> findOverlaps(Long userId, OffsetDateTime startAt, OffsetDateTime endAt, Long excludeEventId) {
        OffsetDateTime effectiveEnd = endAt == null ? startAt : endAt;
        String sql = "SELECT e.event_id, e.company_id, e.event_name, e.start_at, e.end_at, e.event_tag, e.event_memo, " +
                "e.notify_at, e.result, e.created_at FROM events e JOIN companies c ON c.company_id = e.company_id " +
                "WHERE c.user_id = ? AND e.start_at <= ? AND COALESCE(e.end_at, e.start_at) >= ? " +
                (excludeEventId == null ? "" : "AND e.event_id <> ? ") + "ORDER BY e.start_at";
        if (excludeEventId == null) {
            return jdbcTemplate.query(sql, rowMapper, userId, Timestamp.from(effectiveEnd.toInstant()), Timestamp.from(startAt.toInstant()));
        }
        return jdbcTemplate.query(sql, rowMapper, userId, Timestamp.from(effectiveEnd.toInstant()),
                Timestamp.from(startAt.toInstant()), excludeEventId);
    }

    /** 予定を登録し、自動採番されたIDを返す。 */
    public Long create(Event event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO events " +
                    "(company_id, event_name, start_at, end_at, event_tag, event_memo, notify_at, result, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, event.getCompanyId());
            ps.setString(2, event.getEventName());
            ps.setTimestamp(3, Timestamp.from(event.getStartAt().toInstant()));
            setTimestamp(ps, 4, event.getEndAt());
            ps.setString(5, event.getEventTag());
            ps.setString(6, event.getEventMemo());
            setTimestamp(ps, 7, event.getNotifyAt());
            ps.setString(8, event.getResult());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    /** 予定を更新する。 */
    public int update(Event event) {
        return jdbcTemplate.update("UPDATE events SET event_name = ?, start_at = ?, end_at = ?, event_tag = ?, " +
                        "event_memo = ?, notify_at = ?, result = ? WHERE event_id = ? AND company_id = ?",
                event.getEventName(), Timestamp.from(event.getStartAt().toInstant()), toTimestamp(event.getEndAt()),
                event.getEventTag(), event.getEventMemo(), toTimestamp(event.getNotifyAt()), event.getResult(),
                event.getEventId(), event.getCompanyId());
    }

    /** 予定を削除する。 */
    public int delete(Long eventId, Long companyId) {
        return jdbcTemplate.update("DELETE FROM events WHERE event_id = ? AND company_id = ?", eventId, companyId);
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static void setTimestamp(PreparedStatement ps, int index, OffsetDateTime value) throws java.sql.SQLException {
        if (value == null) ps.setTimestamp(index, null);
        else ps.setTimestamp(index, Timestamp.from(value.toInstant()));
    }
}
