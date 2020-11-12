package com.example.profiling.service;

import com.example.profiling.model.SearchResult;
import com.example.profiling.utils.DocumentUtilsImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfilingService {
    public SearchResult profiling(List<MultipartFile> fileList){
        List<String> filesContext = fileList.stream().map(file -> {
            try {
                return new String(file.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }).collect(Collectors.toList());

        DocumentUtilsImpl documentUtils = new DocumentUtilsImpl(filesContext);


        return SearchResult.builder()
                .document(documentUtils.getEssay())
                .build();
    }
}
