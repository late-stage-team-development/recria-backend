package com.example.recria.repository;

import com.example.recria.entity.Reflection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** reflectionsテーブルを操作するRepository。 */
@Repository
public class ReflectionRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReflectionRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 予定IDから面接振り返りを取得する。 */
    public Optional<Reflection> findByEventId(Long eventId) {
        List<Reflection> rows = jdbcTemplate.query(
                "SELECT event_id, reflection_text, updated_at FROM reflections WHERE event_id = ?",
                (rs, rowNum) -> {
                    Reflection reflection = new Reflection();
                    reflection.setEventId(rs.getLong("event_id"));
                    reflection.setReflectionText(rs.getString("reflection_text"));
                    reflection.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return reflection;
                }, eventId);
        return rows.stream().findFirst();
    }

    /** 面接振り返りを新規登録する。 */
    public int create(Reflection reflection) {
        return jdbcTemplate.update("INSERT INTO reflections (event_id, reflection_text, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP)", reflection.getEventId(), reflection.getReflectionText());
    }

    /** 面接振り返りを更新する。 */
    public int update(Reflection reflection) {
        return jdbcTemplate.update("UPDATE reflections SET reflection_text = ?, updated_at = CURRENT_TIMESTAMP WHERE event_id = ?",
                reflection.getReflectionText(), reflection.getEventId());
    }
}

