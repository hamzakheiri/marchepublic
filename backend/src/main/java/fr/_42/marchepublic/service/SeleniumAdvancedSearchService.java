package fr._42.marchepublic.service;

import fr._42.marchepublic.controller.dto.ConsultationRow;
import fr._42.marchepublic.model.Consultation;
import fr._42.marchepublic.repository.ConsultationRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeleniumAdvancedSearchService {

    private final ObjectProvider<WebDriver> webDriverProvider;
    private final ConsultationRepository consultationRepository;
    private final String baseUrl;
    private final String advancedSearchUrl;
    private final long waitSeconds;

    private WebDriver driver;

    public SeleniumAdvancedSearchService(
            ObjectProvider<WebDriver> webDriverProvider,
            ConsultationRepository consultationRepository,
            @Value("${scraper.marches-publics.default-url}") String baseUrl,
            @Value("${scraper.marches-publics.advanced-search-url}") String advancedSearchUrl,
            @Value("${scraper.selenium.page-load-timeout-seconds:30}") long waitSeconds) {
        this.webDriverProvider = webDriverProvider;
        this.consultationRepository = consultationRepository;
        this.baseUrl = baseUrl;
        this.advancedSearchUrl = advancedSearchUrl;
        this.waitSeconds = waitSeconds;
    }

    public String scrapeSearchPage() {
        initAndNavigate();
        return driver.getPageSource();
    }

    public List<ConsultationRow> scrapeAndParseResults() {
        initAndNavigate();

        Document doc = Jsoup.parse(driver.getPageSource(), driver.getCurrentUrl());
        Element table = doc.selectFirst("table.table-results");
        if (table == null) {
            return List.of();
        }

        List<ConsultationRow> rows = new ArrayList<>();
        for (Element tr : table.select("tbody tr")) {
            ConsultationRow row = parseRow(tr);
            if (row != null) {
                rows.add(row);
                persistIfNew(row);
            }
        }
        return rows;
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
            // plain <div> after category holds the published date
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

    private void persistIfNew(ConsultationRow row) {
        if (row.refConsultation() == null || consultationRepository.existsByRefConsultation(row.refConsultation())) {
            return;
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
