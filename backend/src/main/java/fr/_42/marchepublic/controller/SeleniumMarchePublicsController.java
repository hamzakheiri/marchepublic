package fr._42.marchepublic.controller;

import fr._42.marchepublic.controller.dto.ConsultationRow;
import fr._42.marchepublic.service.SeleniumAdvancedSearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scraper/selenium")
public class SeleniumMarchePublicsController {

    private final SeleniumAdvancedSearchService seleniumAdvancedSearchService;

    public SeleniumMarchePublicsController(SeleniumAdvancedSearchService seleniumAdvancedSearchService) {
        this.seleniumAdvancedSearchService = seleniumAdvancedSearchService;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> advancedSearchGeneral() {
        return ResponseEntity.ok(seleniumAdvancedSearchService.scrapeSearchPage());
    }

    @GetMapping("/results")
    public ResponseEntity<List<ConsultationRow>> advancedSearchResults() {
        return ResponseEntity.ok(seleniumAdvancedSearchService.scrapeAndParseResults());
    }
}
