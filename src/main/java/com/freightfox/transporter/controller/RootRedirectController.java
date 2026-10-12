package com.freightfox.transporter.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sends visitors of the root URL straight to Swagger UI. */
@Controller
@Hidden
public class RootRedirectController {

    @GetMapping("/")
    public String root() {
        return "redirect:/swagger-ui.html";
    }
}
