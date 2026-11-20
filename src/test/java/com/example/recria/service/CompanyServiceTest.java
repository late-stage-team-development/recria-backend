package com.example.recria.service;

import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.CompanySearchRequest;
import com.example.recria.entity.Company;
import com.example.recria.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    private static final Long USER_ID = 1L;

    @Mock
    CompanyRepository companyRepository;

    CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository);
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
}
