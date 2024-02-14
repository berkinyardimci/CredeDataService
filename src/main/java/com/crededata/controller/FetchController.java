package com.crededata.controller;


import com.crededata.service.FetchDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FetchController {

    private final FetchDataService fetchDataService;

    @PostMapping("/fetchData")
    public ResponseEntity<Boolean> getData() {
        fetchDataService.fetchData();
        return ResponseEntity.ok(true);
    }

    @GetMapping("/getAllTender")
    public ResponseEntity<List<String>> getDataDetail() {
        return ResponseEntity.ok(fetchDataService.getAllTender());
    }

    @GetMapping("/advert")
    public ResponseEntity<String> getAdvertisementByUrl(@RequestParam String url) {
        return ResponseEntity.ok(fetchDataService.getByUrl(url));
    }
}
