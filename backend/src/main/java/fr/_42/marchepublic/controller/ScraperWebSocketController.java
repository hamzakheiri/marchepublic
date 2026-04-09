package fr._42.marchepublic.controller;

import fr._42.marchepublic.model.ScraperConfig;
import fr._42.marchepublic.repository.ScraperConfigRepository;
import fr._42.marchepublic.service.SeleniumAdvancedSearchService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ScraperWebSocketController {

    private final SeleniumAdvancedSearchService scraperService;
    private final ScraperConfigRepository scraperConfigRepository;

    public ScraperWebSocketController(SeleniumAdvancedSearchService scraperService,
                                      ScraperConfigRepository scraperConfigRepository) {
        this.scraperService = scraperService;
        this.scraperConfigRepository = scraperConfigRepository;
    }

    @MessageMapping("/scraper/start")
    public void start() {
        ScraperConfig config = scraperConfigRepository.findById(1L)
                .orElseGet(() -> scraperConfigRepository.save(new ScraperConfig()));
        scraperService.scrapeAndParseResults(config.getMaxPages(), config.isStopOnDuplicate(), config.getMaxResults());
    }

    @MessageMapping("/scraper/stop")
    public void stop() {
        scraperService.requestStop();
    }

    @MessageMapping("/scraper/status")
    public void status() {
        scraperService.broadcastStatus();
    }
}
