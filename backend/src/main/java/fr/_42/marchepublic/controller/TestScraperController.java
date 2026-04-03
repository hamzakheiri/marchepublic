package fr._42.marchepublic.controller;

import fr._42.marchepublic.service.TestScraperService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/scraper")
public class TestScraperController {

    private final TestScraperService testScraperService;

    public TestScraperController(TestScraperService testScraperService) {
        this.testScraperService = testScraperService;
    }

    @GetMapping(value = "/popup", produces = MediaType.TEXT_HTML_VALUE)
    public String fetchPopup() {
        return testScraperService.fetchWithCookies("https://www.marchespublics.gov.ma/index.php?page=commun.PopUpDetailLots&orgAccronyme=o8p&refConsultation=989121&lang=");
    }

    @GetMapping(value = "/detail", produces = MediaType.TEXT_HTML_VALUE)
    public String fetchDetail() {
        return testScraperService.fetchWithCookies("https://www.marchespublics.gov.ma/index.php?page=entreprise.EntrepriseDetailConsultation&refConsultation=989336&orgAcronyme=d4q");
    }
}
