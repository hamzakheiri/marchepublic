package fr._42.marchepublic.controller;

import fr._42.marchepublic.model.ScraperConfig;
import fr._42.marchepublic.repository.ScraperConfigRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scraper/config")
public class ScraperConfigController {

    private final ScraperConfigRepository scraperConfigRepository;

    public ScraperConfigController(ScraperConfigRepository scraperConfigRepository) {
        this.scraperConfigRepository = scraperConfigRepository;
    }

    @GetMapping
    public ScraperConfig get() {
        return scraperConfigRepository.findById(1L)
                .orElseGet(() -> scraperConfigRepository.save(new ScraperConfig()));
    }

    @PutMapping
    public ScraperConfig update(@RequestBody ScraperConfig incoming) {
        ScraperConfig config = scraperConfigRepository.findById(1L)
                .orElseGet(ScraperConfig::new);
        config.setEnabled(incoming.isEnabled());
        config.setMaxPages(incoming.getMaxPages());
        config.setStopOnDuplicate(incoming.isStopOnDuplicate());
        config.setIntervalMinutes(incoming.getIntervalMinutes());
        return scraperConfigRepository.save(config);
    }
}
