package com.example.recria.repository;

import com.example.recria.entity.Company;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** companiesテーブルを操作するRepository。 */
@Repository
public class CompanyRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Company> rowMapper = (rs, rowNum) -> {
        Company company = new Company();
        company.setCompanyId(rs.getLong("company_id"));
        company.setUserId(rs.getLong("user_id"));
        company.setCompanyName(rs.getString("company_name"));
        company.setCompanyInfo(rs.getString("company_info"));
        company.setCompanyMemo(rs.getString("company_memo"));
        company.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        company.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return company;
    };

    public CompanyRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** ログインユーザーが登録した企業一覧を取得する。 */
    public List<Company> findAll(Long userId) {
        return jdbcTemplate.query("SELECT company_id, user_id, company_name, company_info, company_memo, created_at, updated_at " +
                "FROM companies WHERE user_id = ? ORDER BY updated_at DESC", rowMapper, userId);
    }

    /** 企業名のキーワードで検索する。 */
    public List<Company> findAll(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll(userId);
        return jdbcTemplate.query("SELECT company_id, user_id, company_name, company_info, company_memo, created_at, updated_at " +
                        "FROM companies WHERE user_id = ? AND company_name ILIKE ? ORDER BY updated_at DESC",
                rowMapper, userId, "%" + keyword.trim() + "%");
    }

    /** 企業IDから企業を取得する。 */
    public Optional<Company> findById(Long companyId) {
        return jdbcTemplate.query("SELECT company_id, user_id, company_name, company_info, company_memo, created_at, updated_at " +
                "FROM companies WHERE company_id = ?", rowMapper, companyId).stream().findFirst();
    }

    /** 企業を登録し、自動採番されたIDを返す。 */
    public Long create(Company company) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO companies " +
                    "(user_id, company_name, company_info, company_memo, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, company.getUserId());
            ps.setString(2, company.getCompanyName());
            ps.setString(3, company.getCompanyInfo());
            ps.setString(4, company.getCompanyMemo());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    /** 企業情報を更新する。所有者IDも条件に含める。 */
    public int update(Company company) {
        return jdbcTemplate.update("UPDATE companies SET company_name = ?, company_info = ?, company_memo = ?, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE company_id = ? AND user_id = ?",
                company.getCompanyName(), company.getCompanyInfo(), company.getCompanyMemo(),
                company.getCompanyId(), company.getUserId());
    }

    /** ログインユーザー本人の企業のみ削除する。 */
    public int delete(Long companyId, Long userId) {
        return jdbcTemplate.update("DELETE FROM companies WHERE company_id = ? AND user_id = ?", companyId, userId);
    }
}

