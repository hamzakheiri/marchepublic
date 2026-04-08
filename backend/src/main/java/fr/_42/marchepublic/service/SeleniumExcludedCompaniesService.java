package fr._42.marchepublic.service;

import fr._42.marchepublic.controller.dto.ExcludedCompany;
import fr._42.marchepublic.repository.ExcludedCompanyRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SeleniumExcludedCompaniesService {

    private final ObjectProvider<WebDriver> webDriverProvider;
    private final ExcludedCompanyRepository excludedCompanyRepository;
    private final String baseUrl;
    private final String excludedCompaniesUrl;
    private final long waitSeconds;

    private WebDriver driver;

    public SeleniumExcludedCompaniesService(
            ObjectProvider<WebDriver> webDriverProvider,
            ExcludedCompanyRepository excludedCompanyRepository,
            @Value("${scraper.marches-publics.default-url}") String baseUrl,
            @Value("${scraper.marches-publics.excluded-companies-url}") String excludedCompaniesUrl,
            @Value("${scraper.selenium.page-load-timeout-seconds:30}") long waitSeconds) {
        this.webDriverProvider = webDriverProvider;
        this.excludedCompanyRepository = excludedCompanyRepository;
        this.baseUrl = baseUrl;
        this.excludedCompaniesUrl = excludedCompaniesUrl;
        this.waitSeconds = waitSeconds;
    }

    public String scrapeSearchPage() {
        initAndNavigate();
        return driver.getPageSource();
    }

    public List<ExcludedCompany> scrapeAndParseTable() {
        initAndNavigate();
        Document doc = Jsoup.parse(driver.getPageSource(), excludedCompaniesUrl);
        return parseTable(doc);
    }

    public int scrapeAndPersist() {
        initAndNavigate();
        Document doc = Jsoup.parse(driver.getPageSource(), excludedCompaniesUrl);

        int saved = 0;
        for (ExcludedCompany dto : parseTable(doc)) {
            Integer docId = extractDocumentId(dto.documentUrl());
            if (docId == null) {
                log.warn("Skipping row with no document ID: {}", dto.raisonSociale());
                continue;
            }
            if (excludedCompanyRepository.existsByDocumentId(docId)) {
                log.debug("Duplicate skipped: documentId={}", docId);
                continue;
            }

            fr._42.marchepublic.model.ExcludedCompany entity = new fr._42.marchepublic.model.ExcludedCompany();
            entity.setDocumentId(docId);
            entity.setEntitePublique(dto.entitePublique());
            entity.setRegistreCommerce(dto.registreCommerce());
            entity.setRaisonSociale(dto.raisonSociale());
            entity.setMotif(dto.motif());
            entity.setDateDebut(dto.dateDebut());
            entity.setDateFin(dto.dateFin());
            entity.setPortee(dto.portee());
            entity.setDocumentUrl(dto.documentUrl());
            entity.setDocumentInfo(dto.documentInfo());
            excludedCompanyRepository.save(entity);
            log.info("Saved excluded company [id={}]: {}", docId, dto.raisonSociale());
            saved++;
        }
        return saved;
    }

    private List<ExcludedCompany> parseTable(Document doc) {
        List<ExcludedCompany> results = new ArrayList<>();
        Element table = doc.selectFirst("table.table-results");
        if (table == null) return results;

        for (Element tr : table.select("tbody tr")) {
            ExcludedCompany row = parseRow(tr);
            if (row != null) results.add(row);
        }
        return results;
    }

    private ExcludedCompany parseRow(Element tr) {
        String entitePublique = text(tr, "td[headers=entitePublique]");
        String registreCommerce = text(tr, "td[headers=registreCommerce] .objet-line");
        String raisonSociale = text(tr, "td[headers=libelleFournisseur] .objet-line");
        String motif = text(tr, "td[headers=motif] .objet-line");

        if (entitePublique == null && raisonSociale == null) return null;

        String dateDebut = null;
        String dateFin = null;
        String portee = null;
        Element dateCell = tr.selectFirst("td[headers=dateDebutExclusion]");
        if (dateCell != null) {
            Elements ltrSpans = dateCell.select("span.ltr");
            if (ltrSpans.size() >= 1) dateDebut = clean(ltrSpans.get(0).text());
            if (ltrSpans.size() >= 2) dateFin = clean(ltrSpans.get(1).text());
            Element spacer = dateCell.selectFirst("div.spacer-mini");
            if (spacer != null && spacer.nextSibling() != null) {
                portee = clean(spacer.nextSibling().toString());
            }
        }

        String documentUrl = null;
        String documentInfo = null;
        Element docCell = tr.selectFirst("td[headers=document]");
        if (docCell != null) {
            Element link = docCell.selectFirst("a[href]");
            if (link != null) documentUrl = resolveDocumentUrl(link.attr("href"));
            Element infoSpan = docCell.selectFirst("span.ltr");
            if (infoSpan != null) documentInfo = clean(infoSpan.text());
        }

        return new ExcludedCompany(
                entitePublique, registreCommerce, raisonSociale,
                motif, dateDebut, dateFin, portee,
                documentUrl, documentInfo
        );
    }

    private void initAndNavigate() {
        if (driver == null) {
            driver = webDriverProvider.getObject();
            driver.get(baseUrl);
            waitForPageReady(driver);
        }

        driver.get(excludedCompaniesUrl);
        waitForPageReady(driver);

        clickLaunchSearch(driver);
        waitForPageReady(driver);

        setMaxPageSize();
        waitForPageReady(driver);
    }

    private void setMaxPageSize() {
        try {
            // Find any <select> that has a "500" option and select it
            List<WebElement> selects = driver.findElements(By.tagName("select"));
            for (WebElement selectEl : selects) {
                Select select = new Select(selectEl);
                boolean has500 = select.getOptions().stream()
                        .anyMatch(o -> "500".equals(o.getAttribute("value")) || "500".equals(o.getText().trim()));
                if (has500) {
                    select.selectByVisibleText("500");
                    log.info("Page size set to 500");
                    return;
                }
            }
            log.warn("No page-size select with option 500 found — proceeding with default");
        } catch (Exception e) {
            log.warn("Could not set page size: {}", e.getMessage());
        }
    }

    private Integer extractDocumentId(String documentUrl) {
        if (documentUrl == null) return null;
        int idx = documentUrl.lastIndexOf("id=");
        if (idx < 0) return null;
        try {
            return Integer.parseInt(documentUrl.substring(idx + 3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveDocumentUrl(String jsHref) {
        // javascript:popUp('?page=agent.DownloadDocumentSocietesExclues&id=4','yes')
        int start = jsHref.indexOf("'");
        int end = jsHref.indexOf("'", start + 1);
        if (start < 0 || end <= start) return null;
        String relative = jsHref.substring(start + 1, end).replace("&amp;", "&");
        return baseUrl + "/" + relative.replaceFirst("^/", "");
    }

    private String text(Element parent, String cssSelector) {
        Element el = parent.selectFirst(cssSelector);
        return el != null ? clean(el.text()) : null;
    }

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void clickLaunchSearch(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("input[type=submit], button, input[type=button]")));

        List<WebElement> controls = driver.findElements(
                By.cssSelector("input[type=submit], button, input[type=button]"));

        for (WebElement control : controls) {
            String label = control.getAttribute("value");
            if (label == null || label.isBlank()) label = control.getText();
            if (label != null && normalize(label).contains("lancer la recherche")) {
                new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                        .until(ExpectedConditions.elementToBeClickable(control));
                js.executeScript("arguments[0].scrollIntoView(true);", control);
                try {
                    control.click();
                } catch (Exception e) {
                    js.executeScript("arguments[0].click();", control);
                }
                return;
            }
        }
    }

    private void waitForPageReady(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds)).until(d -> {
            boolean domReady = "complete".equals(js.executeScript("return document.readyState"));
            Object jQueryActive = js.executeScript(
                    "return (typeof jQuery !== 'undefined') ? jQuery.active : 0");
            boolean jqueryIdle = jQueryActive == null || ((Long) jQueryActive) == 0;
            return domReady && jqueryIdle;
        });
    }

    private String normalize(String value) {
        return value
                .replace('\u00a0', ' ')
                .toLowerCase()
                .replace('é', 'e').replace('è', 'e').replace('ê', 'e')
                .replace('à', 'a').replace('ù', 'u')
                .trim();
    }
}
