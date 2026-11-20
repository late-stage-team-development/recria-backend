package com.example.recria.controller;

import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.CompanySearchRequest;
import com.example.recria.repository.UserRepository;
import com.example.recria.service.CompanyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/** 企業の一覧・詳細・登録・編集・削除の画面を扱うController。 */
@Controller
public class CompanyController {
    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyService companyService;
    private final UserRepository userRepository;

    public CompanyController(CompanyService companyService, UserRepository userRepository) {
        this.companyService = companyService;
        this.userRepository = userRepository;
    }

    /** API-05 企業一覧・検索 */
    @GetMapping("/company")
    public String list(@Valid @ModelAttribute("search") CompanySearchRequest search, BindingResult bindingResult,
                       Authentication auth, Model model) {
        List<CompanyResponse> companies = bindingResult.hasErrors()
                ? List.of()
                : companyService.search(currentUserId(auth), search);
        model.addAttribute("companies", companies);
        model.addAttribute("searched", search.getKeyword() != null && !search.getKeyword().isBlank());
        return "company/list";
    }

    /** API-06 企業詳細（紐づく予定一覧も表示） */
    @GetMapping("/company/{companyId}")
    public String detail(@PathVariable Long companyId, Authentication auth, Model model) {
        model.addAttribute("company", companyService.getDetail(currentUserId(auth), companyId));
        return "company/detail";
    }

    /**
     * ログイン中のユーザーIDを取得する。
     * 認証（A担当）の実装が決まったら、ここだけ差し替える。
     */
    private Long currentUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getUserId();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleStatus(ResponseStatusException e) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            return notFound(e.getReason());
        }
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return new ModelAndView("redirect:/login");
        }
        return unexpected(e);
    }

    /** /company/abc のようにIDが数値でない場合も「企業情報が見つかりません」とする。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return notFound(CompanyService.COMPANY_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e) {
        return unexpected(e);
    }

    private ModelAndView notFound(String message) {
        ModelAndView mav = new ModelAndView("error/404", HttpStatus.NOT_FOUND);
        mav.addObject("errorMessage", message);
        return mav;
    }

    private ModelAndView unexpected(Exception e) {
        log.error("想定外のエラーが発生しました", e);
        return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
