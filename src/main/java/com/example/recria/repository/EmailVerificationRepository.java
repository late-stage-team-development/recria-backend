package com.example.recria.repository;

import com.example.recria.entity.EmailVerification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** email_verificationsテーブルを操作するRepository。 */
@Repository
public class EmailVerificationRepository {
    private final JdbcTemplate jdbcTemplate;

    public EmailVerificationRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** ユーザーIDから認証コードを取得する。 */
    public Optional<EmailVerification> findByUserId(Long userId) {
        List<EmailVerification> rows = jdbcTemplate.query(
                "SELECT verification_id, user_id, code, expires_at FROM email_verifications WHERE user_id = ?",
                (rs, rowNum) -> {
                    EmailVerification value = new EmailVerification();
                    value.setVerificationId(rs.getLong("verification_id"));
                    value.setUserId(rs.getLong("user_id"));
                    value.setCode(rs.getString("code"));
                    value.setExpiresAt(rs.getObject("expires_at", java.time.OffsetDateTime.class));
                    return value;
                }, userId);
        return rows.stream().findFirst();
    }

    /** 認証コードを保存する。既存レコードがあれば再送として上書きする。 */
    public void save(EmailVerification verification) {
        int updated = jdbcTemplate.update(
                "UPDATE email_verifications SET code = ?, expires_at = ? WHERE user_id = ?",
                verification.getCode(), Timestamp.from(verification.getExpiresAt().toInstant()), verification.getUserId());
        if (updated == 0) {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO email_verifications (user_id, code, expires_at) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, verification.getUserId());
                ps.setString(2, verification.getCode());
                ps.setTimestamp(3, Timestamp.from(verification.getExpiresAt().toInstant()));
                return ps;
            }, new org.springframework.jdbc.support.GeneratedKeyHolder());
        }
    }

    /** ユーザーIDに紐づく認証コードを削除する。 */
    public int deleteByUserId(Long userId) {
        return jdbcTemplate.update("DELETE FROM email_verifications WHERE user_id = ?", userId);
    }
}
