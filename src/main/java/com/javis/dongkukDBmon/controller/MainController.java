package com.javis.dongkukDBmon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/{path:[^\\.]*}") // .html 등 파일 요청 제외
    public String forward() {
        return "forward:/";
    }
}