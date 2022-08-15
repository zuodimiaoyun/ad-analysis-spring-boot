package com.kiona.analysis.controller;

import com.kiona.analysis.facebook.service.FacebookAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yangshuaichao
 * @date 2022/08/15 15:15
 * @description TODO
 */
@RestController
@RequestMapping("/api/v1/facebook")
public class FacebookAnalysisController {
    private final FacebookAnalysisService facebookAnalysisService;

    public FacebookAnalysisController(FacebookAnalysisService facebookAnalysisService) {this.facebookAnalysisService = facebookAnalysisService;}

    @PostMapping("/analysis")
    public void analysis(MultipartFile file, HttpServletResponse response) throws IOException {
        facebookAnalysisService.analysis(file, response);
    }
}
