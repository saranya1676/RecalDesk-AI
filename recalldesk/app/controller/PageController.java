package com.recalldesk.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the single-page HTML frontend.
 * All routes are handled client-side via JavaScript.
 */
@Controller
public class PageController {

    @GetMapping({"/", "/dashboard", "/customers", "/chat", "/memory", "/analytics",
                 "/memory-impact", "/settings", "/demo"})
    public String index() {
        return "index";
    }
}
