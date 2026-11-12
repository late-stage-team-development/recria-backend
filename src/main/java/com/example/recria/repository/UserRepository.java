package com.example.recria.repository;

import com.example.recria.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** usersテーブルを操作するRepository。 */
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return user;
    };

    public UserRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** メールアドレスからユーザーを取得する。 */
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query("SELECT user_id, email, password, email_verified, created_at FROM users WHERE email = ?",
                rowMapper, email).stream().findFirst();
    }

    /** ユーザーを登録し、自動採番されたIDを返す。 */
    public Long create(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, password, email_verified, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setBoolean(3, Boolean.TRUE.equals(user.getEmailVerified()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    /** メール認証完了時に認証済みフラグをtrueにする。 */
    public int updateEmailVerified(Long userId) {
        return jdbcTemplate.update("UPDATE users SET email_verified = TRUE WHERE user_id = ?", userId);
    }

    /** ユーザーIDからユーザーを取得する。 */
    public Optional<User> findById(Long userId) {
        return jdbcTemplate.query("SELECT user_id, email, password, email_verified, created_at FROM users WHERE user_id = ?",
                rowMapper, userId).stream().findFirst();
    }
}
