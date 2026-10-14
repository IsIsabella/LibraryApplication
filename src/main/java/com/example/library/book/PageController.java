package com.example.library.book;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Контроллер для страницы /itline
 *
 * @author e.basalaeva
 */
@Controller
@ThreadSafe
public class PageController {

    /**
     * @return отправка на /library.html
     */
    @GetMapping("/itline")
    public String page() {
        return "forward:/library.html";
    }
}
