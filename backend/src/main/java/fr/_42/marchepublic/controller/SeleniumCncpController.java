package fr._42.marchepublic.controller;

import fr._42.marchepublic.model.CncpAvis;
import fr._42.marchepublic.service.SeleniumCncpService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scraper/selenium/cncp")
public class SeleniumCncpController {

    private final SeleniumCncpService seleniumCncpService;

    public SeleniumCncpController(SeleniumCncpService seleniumCncpService) {
        this.seleniumCncpService = seleniumCncpService;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> scrapeRawHtml() {
        return ResponseEntity.ok(seleniumCncpService.scrapeSearchPage());
    }

    @GetMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CncpAvis>> scrapeData() {
        return ResponseEntity.ok(seleniumCncpService.scrapeAndParseTable());
    }

    @PostMapping(value = "/scrape", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> scrapeAndPersist() {
        int saved = seleniumCncpService.scrapeAndPersist();
        return ResponseEntity.ok(Map.of("saved", saved));
    }
}
