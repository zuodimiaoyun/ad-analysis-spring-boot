package com.kiona.analysis.controller;

import com.kiona.analysis.google.constant.GoogleAnalysisType;
import com.kiona.analysis.google.service.GoogleAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yangshuaichao
 * @date 2022/08/15 11:42
 * @description TODO
 */
@RestController
@RequestMapping("/api/v1/google")
public class GoogleAnalysisController {

    private final GoogleAnalysisService googleAnalysisService;

    public GoogleAnalysisController(GoogleAnalysisService googleAnalysisService) {this.googleAnalysisService = googleAnalysisService;}

    @PostMapping("/analysis")
    public void analysis(MultipartFile file, @RequestParam(defaultValue = GoogleAnalysisType.ROE) String analysisType, HttpServletResponse response) throws IOException {
        googleAnalysisService.analysis(file, analysisType, response);
    }
}
