package fr._42.marchepublic.controller;

import fr._42.marchepublic.model.ScraperConfig;
import fr._42.marchepublic.repository.ScraperConfigRepository;
import fr._42.marchepublic.service.SeleniumAdvancedSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/scraper")
public class ScraperRestController {

    private final SeleniumAdvancedSearchService scraperService;
    private final ScraperConfigRepository scraperConfigRepository;

    public ScraperRestController(SeleniumAdvancedSearchService scraperService,
                                 ScraperConfigRepository scraperConfigRepository) {
        this.scraperService = scraperService;
        this.scraperConfigRepository = scraperConfigRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start(
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(required = false) Integer maxPages
    ) {
        ScraperConfig config = scraperConfigRepository.findById(1L)
                .orElseGet(() -> scraperConfigRepository.save(new ScraperConfig()));
        int effectiveMaxResults = maxResults != null ? maxResults : config.getMaxResults();
        int effectiveMaxPages = maxPages != null ? maxPages : config.getMaxPages();
        scraperService.scrapeAndParseResults(effectiveMaxPages, config.isStopOnDuplicate(), effectiveMaxResults);
        return ResponseEntity.accepted().body(Map.of("status", "STARTING"));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stop() {
        scraperService.requestStop();
        return ResponseEntity.ok(Map.of("status", "STOP_REQUESTED"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", scraperService.isRunning() ? "RUNNING" : "IDLE"));
    }
}
