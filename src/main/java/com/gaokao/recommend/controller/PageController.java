package com.gaokao.recommend.controller;

import com.gaokao.recommend.service.RecommendService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final RecommendService recommendService;

    public PageController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/recommend"})
    public String index(Model model) {
        model.addAttribute("schoolProvinces", recommendService.getSchoolProvinces());
        return "index";
    }
}
