package com.norkts.icros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/web")
    public String index() {
        return "index";
    }
}
