package com.example.profiling.controller;

import com.example.profiling.service.ProfilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping()
public class ProfilingController {

    private final ProfilingService profilingService;

    @PostMapping("profiling")
    public String getLanguageOfText(@RequestParam(value = "filesToProfiling") List<MultipartFile> files,
                                    Model model) {
        model.addAttribute("searchResult", profilingService.profiling(files));

        return "start";
    }

    @GetMapping
    public String mainPage(){
        return "start";
    }
}
