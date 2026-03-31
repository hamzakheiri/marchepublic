package fr._42.marchepublic.controller;

import fr._42.marchepublic.controller.dto.AdvancedSearchStep1Response;
import fr._42.marchepublic.controller.dto.AdvancedSearchStep2Response;
import fr._42.marchepublic.controller.dto.MarcheNoticeResponse;
import fr._42.marchepublic.service.MarchePublicsAdvancedSearchScraperService;
import fr._42.marchepublic.service.MarchePublicsScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scraper")
public class MarchePublicsController {

    private final MarchePublicsScraperService scraperService;
    private final MarchePublicsAdvancedSearchScraperService advancedSearchScraperService;

    public MarchePublicsController(
            MarchePublicsScraperService scraperService,
            MarchePublicsAdvancedSearchScraperService advancedSearchScraperService) {
        this.scraperService = scraperService;
        this.advancedSearchScraperService = advancedSearchScraperService;
    }

    @GetMapping("/marchespublics")
    public ResponseEntity<List<MarcheNoticeResponse>> scrape(
            @RequestParam(required = false) String url,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(scraperService.scrape(url, limit));
    }

    @GetMapping("/content")
    public ResponseEntity<List<MarcheNoticeResponse>> content(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(scraperService.scrape(null, limit));
    }

    @GetMapping("/advanced-search/step-1")
    public ResponseEntity<AdvancedSearchStep1Response> advancedSearchStep1() {
        return ResponseEntity.ok(advancedSearchScraperService.scrapeStep1());
    }

    @GetMapping("/advanced-search/step-2")
    public ResponseEntity<AdvancedSearchStep2Response> advancedSearchStep2() {
        return ResponseEntity.ok(advancedSearchScraperService.scrapeStep2ClickSearch());
    }

    @GetMapping("/advanced-search/table")
    public ResponseEntity<AdvancedSearchStep2Response.TableData> advancedSearchTable() {
        return ResponseEntity.ok(advancedSearchScraperService.scrapeStep2TableOnly());
    }
}
