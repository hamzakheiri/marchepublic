package fr._42.marchepublic.service;

import fr._42.marchepublic.model.CncpAvis;
import fr._42.marchepublic.repository.CncpAvisRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SeleniumCncpService {

    private static final String TABLE_ID = "2861";

    private final ObjectProvider<WebDriver> webDriverProvider;
    private final CncpAvisRepository cncpAvisRepository;
    private final String cncpUrl;
    private final long waitSeconds;

    private WebDriver driver;

    public SeleniumCncpService(
            ObjectProvider<WebDriver> webDriverProvider,
            CncpAvisRepository cncpAvisRepository,
            @Value("${scraper.cncp.url}") String cncpUrl,
            @Value("${scraper.selenium.page-load-timeout-seconds:30}") long waitSeconds) {
        this.webDriverProvider = webDriverProvider;
        this.cncpAvisRepository = cncpAvisRepository;
        this.cncpUrl = cncpUrl;
        this.waitSeconds = waitSeconds;
    }

    public String scrapeSearchPage() {
        initAndNavigate();
        return driver.getPageSource();
    }

    public List<CncpAvis> scrapeAndParseTable() {
        log.info("[CNCP] Starting scrapeAndParseTable");
        initAndNavigate();
        log.info("[CNCP] Navigation done, waiting for DataTable rows");
        waitForDataTableRows();
        log.info("[CNCP] Rows visible, setting max page size");
        setMaxPageSize();
        waitForDataTableRows();
        log.info("[CNCP] Parsing all pages");
        List<CncpAvis> results = parseAllPages();
        log.info("[CNCP] Done — {} rows parsed", results.size());
        return results;
    }

    public int scrapeAndPersist() {
        log.info("[CNCP] Starting scrapeAndPersist");
        initAndNavigate();
        waitForDataTableRows();
        setMaxPageSize();
        waitForDataTableRows();

        int saved = 0;
        for (CncpAvis avis : parseAllPages()) {
            if (avis.getNumeroAvis() == null || cncpAvisRepository.existsByNumeroAvis(avis.getNumeroAvis())) {
                log.debug("[CNCP] Duplicate skipped: {}", avis.getNumeroAvis());
                continue;
            }
            cncpAvisRepository.save(avis);
            log.info("[CNCP] Saved avis [{}]: {}", avis.getNumeroAvis(), avis.getObjet());
            saved++;
        }
        log.info("[CNCP] Persist done — {} new records saved", saved);
        return saved;
    }

    private List<CncpAvis> parseAllPages() {
        List<CncpAvis> all = new ArrayList<>();
        int page = 1;

        while (true) {
            log.info("[CNCP] Parsing page {}", page);

            // Read rows directly from the live DOM via JavaScript (avoids Jsoup missing dynamic rows)
            Document doc = Jsoup.parse(driver.getPageSource(), cncpUrl);
            Element tbody = doc.selectFirst("table#" + TABLE_ID + " tbody");

            if (tbody == null) {
                log.warn("[CNCP] tbody not found in page source on page {}", page);
            } else {
                int before = all.size();
                for (Element tr : tbody.select("tr")) {
                    CncpAvis avis = parseRow(tr);
                    if (avis != null) all.add(avis);
                }
                log.info("[CNCP] Page {} — {} new rows ({} total)", page, all.size() - before, all.size());
            }

            // DataTables next button
            try {
                WebElement next = driver.findElement(By.id(TABLE_ID + "_next"));
                String cls = next.getAttribute("class");
                log.info("[CNCP] Next button classes: '{}'", cls);
                if (cls != null && cls.contains("disabled")) {
                    log.info("[CNCP] Last page reached");
                    break;
                }
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", next);
                waitForDataTableRows();
                page++;
            } catch (Exception e) {
                log.warn("[CNCP] Could not advance to next page: {}", e.getMessage());
                break;
            }
        }

        return all;
    }

    private CncpAvis parseRow(Element tr) {
        List<Element> cells = tr.select("td");
        if (cells.size() < 3) {
            log.debug("[CNCP] Skipping row with {} cells", cells.size());
            return null;
        }

        String numeroAvis = clean(cells.get(0).text());
        String date = clean(cells.get(1).text());
        String objet = clean(cells.get(2).text());

        log.debug("[CNCP] Row parsed — numero='{}' date='{}' objet='{}'", numeroAvis, date, objet);

        if (numeroAvis == null && objet == null) return null;

        // Look for a document link in any cell
        String documentUrl = null;
        for (Element cell : cells) {
            Element link = cell.selectFirst("a[href]");
            if (link != null) {
                String href = link.attr("abs:href");
                if (href == null || href.isBlank()) href = link.attr("href");
                if (!href.isBlank()) {
                    documentUrl = href;
                    break;
                }
            }
        }

        CncpAvis avis = new CncpAvis();
        avis.setNumeroAvis(numeroAvis);
        avis.setDate(date);
        avis.setObjet(objet);
        avis.setDocumentUrl(documentUrl);
        return avis;
    }

    private void setMaxPageSize() {
        try {
            WebElement selectEl = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                    .until(ExpectedConditions.presenceOfElementLocated(By.name(TABLE_ID + "_length")));
            Select select = new Select(selectEl);
            List<WebElement> options = select.getOptions();
            WebElement last = options.get(options.size() - 1);
            select.selectByValue(last.getAttribute("value"));
            log.info("[CNCP] DataTable page size set to {}", last.getText());
        } catch (Exception e) {
            log.warn("[CNCP] Could not set DataTable page size: {}", e.getMessage());
        }
    }

    /**
     * Waits for DataTables to finish its AJAX load and render at least one <tr> in the tbody.
     * Uses a JS-level check so we don't race against the AJAX callback.
     */
    private void waitForDataTableRows() {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds)).until(d -> {
            try {
                // Check DataTables API directly: initialized + has data
                Object count = js.executeScript(
                        "var tbl = $('#" + TABLE_ID + "');" +
                        "if (!tbl.length || !$.fn.dataTable.isDataTable(tbl)) return false;" +
                        "return tbl.DataTable().data().count() > 0;"
                );
                if (Boolean.TRUE.equals(count)) {
                    log.debug("[CNCP] DataTable reports {} rows", count);
                    return true;
                }
            } catch (Exception ignored) {}

            // Fallback: check for real <tr> elements in the DOM (not just comments)
            return !driver.findElements(By.cssSelector("table#" + TABLE_ID + " tbody tr")).isEmpty();
        });

        log.info("[CNCP] DataTable rows confirmed in DOM");
    }

    private void initAndNavigate() {
        if (driver == null) {
            driver = webDriverProvider.getObject();
        }
        log.info("[CNCP] Navigating to {}", cncpUrl);
        driver.get(cncpUrl);
        waitForPageReady(driver);
        log.info("[CNCP] Page ready");
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

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
