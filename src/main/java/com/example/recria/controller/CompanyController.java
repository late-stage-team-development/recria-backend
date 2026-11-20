package com.example.recria.controller;

import com.example.recria.dto.CompanyRegisterRequest;
import com.example.recria.dto.CompanyRequest;
import com.example.recria.dto.CompanyResponse;
import com.example.recria.dto.CompanySearchRequest;
import com.example.recria.dto.EventResponse;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    /** 画面の datetime-local（例: 2026-12-01T10:00）を日本時間の OffsetDateTime として扱う。 */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(OffsetDateTime.class, new TokyoDateTimeEditor());
    }

    /** API-04 企業＋最初の予定の登録（表示） */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new CompanyRegisterRequest());
        return "company/register";
    }

    /**
     * API-04 企業＋最初の予定の登録（保存）。
     * 同名企業（VAL-03）や時間が重なる予定（FR-06）があれば警告し、
     * 「それでも登録する」（confirmed=true）が選ばれたときだけ登録する。
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") CompanyRegisterRequest form, BindingResult bindingResult,
                           @RequestParam(defaultValue = "false") boolean confirmed,
                           Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "company/register";
        }
        Long userId = currentUserId(auth);
        if (!confirmed) {
            boolean duplicate = companyService.hasDuplicateName(userId, form.getCompanyName(), null);
            List<EventResponse> overlaps = companyService.findOverlaps(userId, form.getStartAt(), form.getEndAt());
            if (duplicate || !overlaps.isEmpty()) {
                model.addAttribute("duplicateWarning", duplicate);
                model.addAttribute("overlaps", overlaps);
                return "company/register";
            }
        }
        Long companyId = companyService.register(userId, form);
        redirectAttributes.addFlashAttribute("message", "企業と予定を登録しました");
        return "redirect:/company/" + companyId;
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

    /** API-07 企業情報編集（表示） */
    @GetMapping("/company/{companyId}/edit")
    public String editForm(@PathVariable Long companyId, Authentication auth, Model model) {
        model.addAttribute("form", companyService.getForEdit(currentUserId(auth), companyId));
        model.addAttribute("companyId", companyId);
        return "company/edit";
    }

    /** API-07 企業情報編集（保存）。同名企業があれば警告し、確認後に保存する。 */
    @PostMapping("/company/{companyId}/edit")
    public String edit(@PathVariable Long companyId,
                       @Valid @ModelAttribute("form") CompanyRequest form, BindingResult bindingResult,
                       @RequestParam(defaultValue = "false") boolean confirmDuplicate,
                       Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        Long userId = currentUserId(auth);
        companyService.checkOwner(userId, companyId);
        model.addAttribute("companyId", companyId);
        if (bindingResult.hasErrors()) {
            return "company/edit";
        }
        if (!confirmDuplicate && companyService.hasDuplicateName(userId, form.getCompanyName(), companyId)) {
            model.addAttribute("duplicateWarning", true);
            return "company/edit";
        }
        companyService.update(userId, companyId, form);
        redirectAttributes.addFlashAttribute("message", "企業情報を更新しました");
        return "redirect:/company/" + companyId;
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

    /** datetime-local の値と OffsetDateTime（日本時間）を相互変換する。 */
    static class TokyoDateTimeEditor extends PropertyEditorSupport {
        private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        @Override
        public void setAsText(String text) {
            if (text == null || text.isBlank()) {
                setValue(null);
                return;
            }
            try {
                setValue(LocalDateTime.parse(text.strip()).atZone(TOKYO).toOffsetDateTime());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日時の形式が正しくありません: " + text, e);
            }
        }

        @Override
        public String getAsText() {
            OffsetDateTime value = (OffsetDateTime) getValue();
            return value == null ? "" : value.atZoneSameInstant(TOKYO).format(FORMAT);
        }
    }
}
