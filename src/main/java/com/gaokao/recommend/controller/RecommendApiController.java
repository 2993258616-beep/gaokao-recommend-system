package com.gaokao.recommend.controller;

import com.gaokao.recommend.entity.MajorNameStat;
import com.gaokao.recommend.entity.PredictionLine;
import com.gaokao.recommend.service.RecommendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RecommendApiController {
    private final RecommendService recommendService;

    public RecommendApiController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/majors")
    public List<MajorNameStat> majors() {
        return recommendService.getMajors();
    }

    @GetMapping("/school-provinces")
    public List<String> schoolProvinces() {
        return recommendService.getSchoolProvinces();
    }

    @GetMapping("/recommend")
    public Map<String, List<PredictionLine>> recommend(
            @RequestParam(defaultValue = "500") Integer score,
            @RequestParam(defaultValue = "历史") String subjectType,
            @RequestParam(defaultValue = "全部地区") String schoolProvince,
            @RequestParam(defaultValue = "全部专业") String majorName,
            @RequestParam(defaultValue = "0") Integer nonce) {
        return recommendService.recommend(score, subjectType, schoolProvince, majorName, nonce);
    }
}
