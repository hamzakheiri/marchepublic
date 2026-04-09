package fr._42.marchepublic.service;

import fr._42.marchepublic.controller.dto.ConsultationRow;
import fr._42.marchepublic.model.Consultation;
import fr._42.marchepublic.model.ConsultationDocument;
import fr._42.marchepublic.model.Lot;
import fr._42.marchepublic.repository.ConsultationDocumentRepository;
import fr._42.marchepublic.repository.ConsultationRepository;
import fr._42.marchepublic.repository.LotRepository;
import fr._42.marchepublic.repository.ScraperConfigRepository;
import org.jsoup.Connection;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SeleniumAdvancedSearchService {

    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final ObjectProvider<WebDriver> webDriverProvider;
    private final ConsultationRepository consultationRepository;
    private final LotRepository lotRepository;
    private final ConsultationDocumentRepository consultationDocumentRepository;
    private final ScraperConfigRepository scraperConfigRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final String baseUrl;
    private final String advancedSearchUrl;
    private final long waitSeconds;

    private WebDriver driver;
    private volatile boolean stopRequested = false;
    private volatile boolean running = false;

    public SeleniumAdvancedSearchService(
            ObjectProvider<WebDriver> webDriverProvider,
            ConsultationRepository consultationRepository,
            LotRepository lotRepository,
            ConsultationDocumentRepository consultationDocumentRepository,
            ScraperConfigRepository scraperConfigRepository,
            SimpMessagingTemplate messagingTemplate,
            @Value("${scraper.marches-publics.default-url}") String baseUrl,
            @Value("${scraper.marches-publics.advanced-search-url}") String advancedSearchUrl,
            @Value("${scraper.selenium.page-load-timeout-seconds:30}") long waitSeconds) {
        this.webDriverProvider = webDriverProvider;
        this.consultationRepository = consultationRepository;
        this.lotRepository = lotRepository;
        this.consultationDocumentRepository = consultationDocumentRepository;
        this.scraperConfigRepository = scraperConfigRepository;
        this.messagingTemplate = messagingTemplate;
        this.baseUrl = baseUrl;
        this.advancedSearchUrl = advancedSearchUrl;
        this.waitSeconds = waitSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public void requestStop() {
        stopRequested = true;
        broadcast("STOP_REQUESTED", "Stop requested — will halt after current consultation");
    }

    public void broadcastStatus() {
        broadcast("STATUS", running ? "RUNNING" : "IDLE");
    }

    public String scrapeSearchPage() {
        initAndNavigate();
        return driver.getPageSource();
    }

    @Async
    public void scrapeAndParseResults(int maxPages, boolean stopOnDuplicate, int maxResults) {
        if (running) {
            broadcast("WARN", "Scraper is already running");
            return;
        }
        running = true;
        stopRequested = false;

        try {
            initAndNavigate();
            setPageSize("50");
            waitForPageReady(driver);

            int totalPages = Math.min(readTotalPages(), maxPages);
            broadcast("STARTED", "Starting scrape: " + totalPages + " pages (50 results/page, stopOnDuplicate=" + stopOnDuplicate
                    + (maxResults > 0 ? ", maxResults=" + maxResults : "") + ")");

            int totalSaved = 0;
            boolean done = false;

            for (int page = 1; page <= totalPages && !done && !stopRequested; page++) {
                broadcast("PAGE_START", "Scraping page " + page + "/" + totalPages);
                Document doc = Jsoup.parse(driver.getPageSource(), driver.getCurrentUrl());
                Element table = doc.selectFirst("table.table-results");
                int pageCount = 0;
                if (table != null) {
                    for (Element tr : table.select("tbody tr")) {
                        if (stopRequested) break;
                        if (maxResults > 0 && totalSaved + pageCount >= maxResults) {
                            broadcast("MAX_RESULTS", "Reached maxResults=" + maxResults + " — stopping early");
                            done = true;
                            break;
                        }
                        ConsultationRow row = parseRow(tr);
                        if (row != null) {
                            boolean isNew = persistIfNew(row);
                            if (!isNew && stopOnDuplicate) {
                                broadcast("DUPLICATE", "Duplicate found [" + row.refConsultation() + "] — stopping early");
                                done = true;
                                break;
                            }
                            if (isNew) pageCount++;
                        }
                    }
                }
                totalSaved += pageCount;
                broadcast("PAGE_DONE", "Page " + page + "/" + totalPages + " done — " + pageCount + " new consultations saved");

                if (!done && !stopRequested && page < totalPages) {
                    clickNextPage();
                    waitForPageReady(driver);
                }
            }

            broadcast("DONE", "Scrape complete — " + totalSaved + " new consultations saved");
            updateRunStatus("SUCCESS");
        } catch (Exception e) {
            log.error("Scrape failed", e);
            broadcast("ERROR", "Scrape failed: " + e.getMessage());
            updateRunStatus("FAILED");
        } finally {
            running = false;
            stopRequested = false;
        }
    }

    private void updateRunStatus(String status) {
        scraperConfigRepository.findById(1L).ifPresent(config -> {
            config.setLastRunAt(LocalDateTime.now());
            config.setLastRunStatus(status);
            scraperConfigRepository.save(config);
        });
    }

    private void broadcast(String type, String message) {
        log.info("[{}] {}", type, message);
        messagingTemplate.convertAndSend("/topic/scraper/logs",
                (Object) Map.of("type", type, "message", message));
    }

    private void setPageSize(String value) {
        WebElement selectEl = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("ctl0_CONTENU_PAGE_resultSearch_listePageSizeBottom")));
        new Select(selectEl).selectByValue(value);
    }

    private int readTotalPages() {
        try {
            WebElement totalEl = driver.findElement(
                    By.id("ctl0_CONTENU_PAGE_resultSearch_nombrePageBottom"));
            return Integer.parseInt(totalEl.getText().trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private void clickNextPage() {
        WebElement nextLink = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.elementToBeClickable(
                        By.id("ctl0_CONTENU_PAGE_resultSearch_PagerBottom_ctl2")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextLink);
    }

    private void initAndNavigate() {
        if (driver == null) {
            driver = webDriverProvider.getObject();
            driver.get(baseUrl);
            waitForPageReady(driver);
        }

        driver.get(advancedSearchUrl);
        waitForPageReady(driver);

        clickLaunchSearch(driver);
        waitForPageReady(driver);
    }

    private boolean persistIfNew(ConsultationRow row) {
        if (row.refConsultation() == null || consultationRepository.existsByRefConsultation(row.refConsultation())) {
            return false;
        }

        Consultation entity = new Consultation();
        entity.setRefConsultation(row.refConsultation());
        entity.setOrgAcronyme(row.orgAcronyme());
        entity.setProcedureType(row.procedureType());
        entity.setProcedureFullName(row.procedureFullName());
        entity.setCategory(row.category());
        entity.setPublishedDate(row.publishedDate());
        entity.setReference(row.reference());
        entity.setObject(row.object());
        entity.setBuyer(row.buyer());
        entity.setLocation(row.location());
        entity.setDeadline(row.deadline());
        entity.setDetailUrl(row.detailUrl());
        entity.setLotsPopupUrl(row.lotsPopupUrl());
        consultationRepository.save(entity);

        log.info("Saved consultation [{}] — {}", entity.getRefConsultation(), entity.getObject());

        if (row.lotsPopupUrl() != null) {
            log.info("  -> Fetching lots for [{}]", entity.getRefConsultation());
            fetchAndPersistLots(entity, row.lotsPopupUrl());
        }

        if (row.detailUrl() != null) {
            log.info("  -> Fetching documents for [{}]", entity.getRefConsultation());
            fetchAndPersistDocuments(entity, row.detailUrl());
        }

        return true;
    }

    private void fetchAndPersistDocuments(Consultation consultation, String detailUrl) {
        try {
            Map<String, String> cookies = extractSeleniumCookies();

            Connection.Response response = Jsoup.connect(detailUrl)
                    .userAgent(USER_AGENT)
                    .timeout((int) (waitSeconds * 1000))
                    .cookies(cookies)
                    .ignoreHttpErrors(true)
                    .execute();

            Document detailDoc = Jsoup.parse(response.body(), detailUrl);

            for (Element li : detailDoc.select("div.bloc-docs-link li.picto-link")) {
                if (li.attr("style").contains("display:none")) continue;

                Element link = li.selectFirst("a[id][href]");
                if (link == null) continue;

                String href = link.attr("href");
                String label = clean(link.text());
                String type = extractDocType(link.id());
                String absoluteUrl = URI.create(detailUrl).resolve(href.replace("&amp;", "&")).toString();

                if (type != null && consultationDocumentRepository.existsByConsultationIdAndType(consultation.getId(), type)) {
                    continue;
                }

                ConsultationDocument document = new ConsultationDocument();
                document.setConsultation(consultation);
                document.setType(type);
                document.setLabel(label);
                document.setUrl(absoluteUrl);
                consultationDocumentRepository.save(document);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch detail page: " + e.getMessage(), e);
        }
    }

    private String extractDocType(String linkId) {
        // e.g. "ctl0_CONTENU_PAGE_linkDownloadDce" -> "dce"
        int idx = linkId.indexOf("linkDownload");
        if (idx < 0) return null;
        return linkId.substring(idx + "linkDownload".length()).toLowerCase();
    }

    private void fetchAndPersistLots(Consultation consultation, String lotsPopupUrl) {
        try {
            Map<String, String> cookies = extractSeleniumCookies();

            Connection.Response response = Jsoup.connect(lotsPopupUrl)
                    .userAgent(USER_AGENT)
                    .timeout((int) (waitSeconds * 1000))
                    .cookies(cookies)
                    .ignoreHttpErrors(true)
                    .execute();

            Document doc = Jsoup.parse(response.body(), lotsPopupUrl);
            parseLots(doc, consultation);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch lots popup: " + e.getMessage(), e);
        }
    }

    private void parseLots(Document doc, Consultation consultation) {
        // Each lot has a cautionProvisoire span — use these to find lot indices
        Elements cautionSpans = doc.select("span[id*=repeaterLots_ctl][id$=_cautionProvisoire]");
        Elements titleDivs = doc.select("div.content-bloc.bloc-600 > div.d-flex");
        Elements boldSpans = doc.select("span.blue.bold");

        for (int i = 0; i < cautionSpans.size(); i++) {
            // Extract the lot index N from the ID: ctl0_CONTENU_PAGE_repeaterLots_ctl{N}_cautionProvisoire
            String cautionId = cautionSpans.get(i).id();
            String lotKey = extractRepeaterKey(cautionId); // e.g. "ctl0"
            if (lotKey == null) continue;

            Integer lotNumber = i + 1;
            // Override with parsed number from span.blue.bold if available
            if (i < boldSpans.size()) {
                String boldText = boldSpans.get(i).text(); // "Lot 1 :"
                try {
                    lotNumber = Integer.parseInt(boldText.replaceAll("[^0-9]", "").trim());
                } catch (NumberFormatException ignored) {}
            }

            if (lotRepository.existsByConsultationIdAndLotNumber(consultation.getId(), lotNumber)) {
                continue;
            }

            String title = i < titleDivs.size() ? clean(titleDivs.get(i).ownText()) : null;
            String estimation = textOf(doc, "span[id*=repeaterLots_" + lotKey + "][id$=labelReferentielZoneText]");
            String caution = textOf(doc, "span[id*=repeaterLots_" + lotKey + "_cautionProvisoire]");
            String qualifications = textOf(doc, "span[id*=repeaterLots_" + lotKey + "_qualification]");
            String agrements = textOf(doc, "span[id*=repeaterLots_" + lotKey + "_agrements]");
            String variante = textOf(doc, "span[id*=repeaterLots_" + lotKey + "_varianteValeur]");
            String consEnv = textOf(doc, "span[id*=repeaterLots_" + lotKey + "_consEnvValeur]");
            String tpePme = textOf(doc, "span[id*=repeaterLots_" + lotKey + "_labelReferentielRadio]");

            // Category: content-bloc after "Catégorie" intitule within this lot's block
            String category = null;
            Element cautionEl = cautionSpans.get(i);
            // Walk up to find the lot block, then search for category
            Element block = cautionEl.closest("div.content");
            if (block != null) {
                for (Element intitule : block.select("div.intitule-bloc")) {
                    if (normalize(intitule.text()).contains("categorie")) {
                        Element next = intitule.nextElementSibling();
                        if (next != null) category = clean(next.text());
                        break;
                    }
                }
            }

            // Visites des lieux: combine all date + address pairs
            String visitesLieux = null;
            Elements dateVisites = doc.select("span[id*=repeaterLots_" + lotKey + "][id*=repeaterVisitesLieux][id$=_dateVisites]");
            Elements adresseVisites = doc.select("span[id*=repeaterLots_" + lotKey + "][id*=repeaterVisitesLieux][id$=_adresseVisites]");
            if (!dateVisites.isEmpty()) {
                List<String> visites = new ArrayList<>();
                for (int j = 0; j < dateVisites.size(); j++) {
                    String date = clean(dateVisites.get(j).text());
                    String addr = j < adresseVisites.size() ? clean(adresseVisites.get(j).text()) : null;
                    if (date != null || addr != null) {
                        visites.add((date != null ? date : "") + (addr != null ? " - " + addr : ""));
                    }
                }
                visitesLieux = String.join(" | ", visites);
            }

            Lot lot = new Lot();
            lot.setConsultation(consultation);
            lot.setLotNumber(lotNumber);
            lot.setTitle(title);
            lot.setCategory(category);
            lot.setEstimation(estimation);
            lot.setCautionProvisoire(caution);
            lot.setQualifications(qualifications);
            lot.setAgrements(agrements);
            lot.setVisitesLieux(visitesLieux);
            lot.setVariante(variante);
            lot.setConsiderationsEnv(consEnv);
            lot.setReserveTpePme(tpePme);
            lotRepository.save(lot);
        }
    }

    private String extractRepeaterKey(String id) {
        // e.g. "ctl0_CONTENU_PAGE_repeaterLots_ctl0_cautionProvisoire" -> "ctl0"
        int idx = id.indexOf("repeaterLots_");
        if (idx < 0) return null;
        String after = id.substring(idx + "repeaterLots_".length()); // "ctl0_cautionProvisoire"
        int underscore = after.indexOf("_");
        return underscore > 0 ? after.substring(0, underscore) : null;
    }

    private Map<String, String> extractSeleniumCookies() {
        Map<String, String> cookies = new HashMap<>();
        for (org.openqa.selenium.Cookie cookie : driver.manage().getCookies()) {
            cookies.put(cookie.getName(), cookie.getValue());
        }
        return cookies;
    }

    private String textOf(Document doc, String cssSelector) {
        Element el = doc.selectFirst(cssSelector);
        return el != null ? clean(el.text()) : null;
    }

    private ConsultationRow parseRow(Element tr) {
        Element refCell = tr.selectFirst("td[headers=cons_ref]");
        Element intituleCell = tr.selectFirst("td[headers=cons_intitule]");
        Element lieuCell = tr.selectFirst("td[headers=cons_lieuExe]");
        Element dateCell = tr.selectFirst("td[headers=cons_dateEnd]");
        Element actionsCell = tr.selectFirst("td.actions[headers=cons_actions]");

        if (refCell == null && intituleCell == null) {
            return null;
        }

        String refConsultation = attrOf(tr, "input[id$=_refCons]", "value");
        String orgAcronyme = attrOf(tr, "input[id$=_orgCons]", "value");

        String procedureType = null;
        String procedureFullName = null;
        String category = null;
        String publishedDate = null;
        if (refCell != null) {
            procedureType = clean(refCell.selectFirst(".line-info-bulle") != null
                    ? refCell.selectFirst(".line-info-bulle").ownText()
                    : null);
            Element procDiv = refCell.selectFirst("[id$=panelBlocTypesProc] div");
            procedureFullName = procDiv != null ? clean(procDiv.text()) : null;
            Element catDiv = refCell.selectFirst("[id$=panelBlocCategorie]");
            category = catDiv != null ? clean(catDiv.text()) : null;
            if (catDiv != null && catDiv.nextElementSibling() != null) {
                publishedDate = clean(catDiv.nextElementSibling().text());
            }
        }

        String reference = null;
        String object = null;
        String buyer = null;
        if (intituleCell != null) {
            Element refSpan = intituleCell.selectFirst("span.ref");
            reference = refSpan != null ? clean(refSpan.text()) : null;

            Element objetDiv = intituleCell.selectFirst("[id$=panelBlocObjet]");
            if (objetDiv != null) {
                Element clone = objetDiv.clone();
                clone.select("strong, .info-suite, .info-bulle").remove();
                object = clean(clone.text());
            }

            Element buyerDiv = intituleCell.selectFirst("[id$=panelBlocDenomination]");
            if (buyerDiv != null) {
                Element clone = buyerDiv.clone();
                clone.select("strong").remove();
                buyer = clean(clone.text());
            }
        }

        String location = null;
        String lotsPopupUrl = null;
        if (lieuCell != null) {
            Element lieuDiv = lieuCell.selectFirst("[id$=panelBlocLieuxExec]");
            if (lieuDiv != null) {
                Element clone = lieuDiv.clone();
                clone.select(".bloc-info-bulle").remove();
                location = clean(clone.text());
            }

            Element lotsLink = lieuCell.selectFirst("a[href*=PopUpDetailLots]");
            if (lotsLink != null) {
                Element img = lotsLink.selectFirst("img");
                boolean visible = img != null && !img.attr("style").contains("display:none");
                if (visible) {
                    lotsPopupUrl = resolvePopupUrl(lotsLink.attr("href"), driver.getCurrentUrl());
                }
            }
        }

        String deadline = null;
        if (dateCell != null) {
            Element clotureDiv = dateCell.selectFirst(".cloture-line");
            if (clotureDiv != null) {
                deadline = clean(clotureDiv.text());
            }
        }

        String detailUrl = null;
        if (actionsCell != null) {
            Element detailLink = actionsCell.selectFirst("a[href*=EntrepriseDetailConsultation]");
            if (detailLink != null) {
                detailUrl = detailLink.absUrl("href");
                if (detailUrl == null || detailUrl.isBlank()) {
                    detailUrl = detailLink.attr("href");
                }
            }
        }

        return new ConsultationRow(
                refConsultation, orgAcronyme,
                procedureType, procedureFullName,
                category, publishedDate,
                reference, object, buyer,
                location, deadline, detailUrl,
                lotsPopupUrl
        );
    }

    private String resolvePopupUrl(String jsHref, String baseUrl) {
        if (jsHref == null) return null;
        int start = jsHref.indexOf("'");
        int end = jsHref.indexOf("'", start + 1);
        if (start < 0 || end <= start) return null;
        String relative = jsHref.substring(start + 1, end).replace("&amp;", "&");
        try {
            return URI.create(baseUrl).resolve(relative).toString();
        } catch (Exception e) {
            return relative;
        }
    }

    private String attrOf(Element parent, String cssSelector, String attr) {
        Element el = parent.selectFirst(cssSelector);
        return el != null ? clean(el.attr(attr)) : null;
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

    private void clickLaunchSearch(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        new WebDriverWait(driver, Duration.ofSeconds(waitSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("input[type=submit], button, input[type=button]")));

        List<WebElement> controls = driver.findElements(
                By.cssSelector("input[type=submit], button, input[type=button]"));

        for (WebElement control : controls) {
            String label = control.getAttribute("value");
            if (label == null || label.isBlank()) {
                label = control.getText();
            }
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

    private String normalize(String value) {
        return value
                .replace('\u00a0', ' ')
                .toLowerCase()
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('ù', 'u')
                .trim();
    }

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
