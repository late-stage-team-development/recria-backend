package com.example.recria.controller;

import com.example.recria.repository.UserRepository;
import com.example.recria.service.HomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

/** ホーム画面を扱うController。 */
@Controller
public class HomeController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final HomeService homeService;
    private final UserRepository userRepository;

    public HomeController(HomeService homeService, UserRepository userRepository) {
        this.homeService = homeService;
        this.userRepository = userRepository;
    }

    /** API-03 ホーム */
    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        model.addAttribute("home", homeService.getHome(currentUserId(auth)));
        return "home";
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
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return new ModelAndView("redirect:/login");
        }
        return handleUnexpected(e);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e) {
        log.error("想定外のエラーが発生しました", e);
        return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
