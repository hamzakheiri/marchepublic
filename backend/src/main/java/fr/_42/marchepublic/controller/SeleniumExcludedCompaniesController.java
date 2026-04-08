package fr._42.marchepublic.controller;

import fr._42.marchepublic.controller.dto.ExcludedCompany;
import fr._42.marchepublic.service.SeleniumExcludedCompaniesService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scraper/selenium/excluded-companies")
public class SeleniumExcludedCompaniesController {

    private final SeleniumExcludedCompaniesService seleniumExcludedCompaniesService;

    public SeleniumExcludedCompaniesController(SeleniumExcludedCompaniesService seleniumExcludedCompaniesService) {
        this.seleniumExcludedCompaniesService = seleniumExcludedCompaniesService;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> scrapeRawHtml() {
        return ResponseEntity.ok(seleniumExcludedCompaniesService.scrapeSearchPage());
    }

    @GetMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ExcludedCompany>> scrapeData() {
        return ResponseEntity.ok(seleniumExcludedCompaniesService.scrapeAndParseTable());
    }

    @PostMapping(value = "/scrape", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> scrapeAndPersist() {
        int saved = seleniumExcludedCompaniesService.scrapeAndPersist();
        return ResponseEntity.ok(Map.of("saved", saved));
    }
}
