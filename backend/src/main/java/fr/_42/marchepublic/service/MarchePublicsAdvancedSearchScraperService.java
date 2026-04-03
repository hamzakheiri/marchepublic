package fr._42.marchepublic.service;

import fr._42.marchepublic.controller.dto.AdvancedSearchStep1Response;
import fr._42.marchepublic.controller.dto.AdvancedSearchStep2Response;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarchePublicsAdvancedSearchScraperService {

    private static final Pattern POPUP_JS_PATTERN = Pattern.compile("^javascript:popUp\\('([^']+)'(?:,'[^']*')?\\)$", Pattern.CASE_INSENSITIVE);

    private final String advancedSearchUrl;
    private final int timeoutMs;

    public MarchePublicsAdvancedSearchScraperService(
            @Value("${scraper.marches-publics.advanced-search-url}") String advancedSearchUrl,
            @Value("${scraper.marches-publics.timeout-ms}") int timeoutMs) {
        this.advancedSearchUrl = advancedSearchUrl;
        this.timeoutMs = timeoutMs;
    }

    public AdvancedSearchStep1Response scrapeStep1() {
        try {
            Document document = Jsoup.connect(advancedSearchUrl)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(timeoutMs)
                    .get();

            Element form = document.selectFirst("form[action]");
            String formAction = form == null ? null : form.absUrl("action");
            boolean launchButtonFound = hasLaunchSearchButton(document);

            return new AdvancedSearchStep1Response(
                    advancedSearchUrl,
                    document.location(),
                    cleanText(document.title()),
                    formAction,
                    launchButtonFound,
                    document.select("form").size()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to fetch advanced search page", exception);
        }
    }

    public AdvancedSearchStep2Response scrapeStep2ClickSearch() {
        return scrapeStep2ClickSearch(true);
    }

    public AdvancedSearchStep2Response scrapeStep2ClickSearch(boolean fetchPopupDetails) {
        try {
            Connection.Response initialResponse = Jsoup.connect(advancedSearchUrl)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(timeoutMs)
                    .method(Connection.Method.GET)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .execute();

            Document initialDocument = initialResponse.parse();
            Element launchControl = findLaunchSearchControl(initialDocument);
            Element form = launchControl != null ? launchControl.closest("form") : null;

            if (form == null) {
                form = initialDocument.selectFirst("form[action]");
            }

            if (form == null) {
                throw new IllegalStateException("No form found on advanced search page");
            }

            String method = cleanText(form.attr("method"));
            String normalizedMethod = method == null ? "GET" : method.toUpperCase();
            String formAction = form.hasAttr("action") ? form.absUrl("action") : initialResponse.url().toString();
            if (formAction == null || formAction.isBlank()) {
                formAction = initialResponse.url().toString();
            }

            Connection submitRequest = Jsoup.connect(formAction)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(timeoutMs)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookies(sanitizeMap(initialResponse.cookies()));

            if ("POST".equals(normalizedMethod)) {
                submitRequest.method(Connection.Method.POST);
            } else {
                submitRequest.method(Connection.Method.GET);
            }

            Map<String, String> formData = extractFormData(form);
            if (launchControl != null) {
                String buttonName = cleanText(launchControl.attr("name"));
                if (buttonName != null) {
                    String buttonValue = cleanText(launchControl.attr("value"));
                    if (buttonValue == null) {
                        buttonValue = cleanText(launchControl.text());
                    }
                    submitRequest.data(buttonName, buttonValue == null ? "" : buttonValue);
                }
            }

            for (Map.Entry<String, String> entry : formData.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    submitRequest.data(entry.getKey(), entry.getValue());
                }
            }

            Connection.Response searchResponse = submitRequest.execute();
            Document resultDocument = Jsoup.parse(searchResponse.body(), searchResponse.url().toString());
            Map<String, String> sessionCookies = sanitizeMap(searchResponse.cookies());
            if (sessionCookies.isEmpty()) {
                sessionCookies = sanitizeMap(initialResponse.cookies());
            }
            List<AdvancedSearchStep2Response.TabLayerData> tabLayers = extractTabLayers(resultDocument);
            AdvancedSearchStep2Response.TableData table = extractPrimaryTable(
                    resultDocument,
                    searchResponse.url().toString()
            );

            if (fetchPopupDetails) {
                enrichRowsWithPopupDetails(table, sessionCookies);
            }

            return new AdvancedSearchStep2Response(
                    advancedSearchUrl,
                    formAction,
                    normalizedMethod,
                    searchResponse.statusCode(),
                    searchResponse.statusMessage(),
                    resultDocument.location(),
                    cleanText(resultDocument.title()),
                    launchControl != null,
                    formData.size(),
                        sessionCookies,
                    tabLayers,
                    table,
                    searchResponse.body()
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to submit advanced search form", exception);
        }
    }

    public AdvancedSearchStep2Response.TableData scrapeStep2TableOnly() {
        return scrapeStep2TableOnly(true);
    }

    public AdvancedSearchStep2Response.TableData scrapeStep2TableOnly(boolean includePopupDetails) {
        AdvancedSearchStep2Response step2 = scrapeStep2ClickSearch(includePopupDetails);
        return step2.getTable();
    }

    private boolean hasLaunchSearchButton(Document document) {
        for (Element buttonLike : document.select("input[type=submit], button, input[type=button]")) {
            String value = cleanText(buttonLike.attr("value"));
            String text = cleanText(buttonLike.text());
            String label = value != null ? value : text;

            if (label == null) {
                continue;
            }

            String normalized = normalize(label);
            if (normalized.contains("lancer la recherche")) {
                return true;
            }
        }

        return normalize(document.text()).contains("lancer la recherche");
    }

    private Element findLaunchSearchControl(Document document) {
        Elements controls = document.select("input[type=submit], button, input[type=button]");
        for (Element control : controls) {
            String value = cleanText(control.attr("value"));
            String text = cleanText(control.text());
            String label = value != null ? value : text;

            if (label == null) {
                continue;
            }

            String normalized = normalize(label);
            if (normalized.contains("lancer la recherche")) {
                return control;
            }
        }

        return null;
    }

    private Map<String, String> extractFormData(Element form) {
        Map<String, String> data = new LinkedHashMap<>();

        for (Element input : form.select("input[name]")) {
            if (input.hasAttr("disabled")) {
                continue;
            }

            String name = cleanText(input.attr("name"));
            if (name == null) {
                continue;
            }

            String type = normalize(input.attr("type"));
            if ("submit".equals(type) || "button".equals(type) || "image".equals(type) || "file".equals(type)) {
                continue;
            }

            if (("checkbox".equals(type) || "radio".equals(type)) && !input.hasAttr("checked")) {
                continue;
            }

            String value = input.hasAttr("value") ? input.attr("value") : "";
            data.put(name, value);
        }

        for (Element textarea : form.select("textarea[name]")) {
            if (textarea.hasAttr("disabled")) {
                continue;
            }

            String name = cleanText(textarea.attr("name"));
            if (name != null) {
                data.put(name, textarea.text());
            }
        }

        for (Element select : form.select("select[name]")) {
            if (select.hasAttr("disabled")) {
                continue;
            }

            String name = cleanText(select.attr("name"));
            if (name == null) {
                continue;
            }

            Element selectedOption = select.selectFirst("option[selected]");
            if (selectedOption == null) {
                selectedOption = select.selectFirst("option");
            }

            String value = selectedOption == null ? "" : selectedOption.attr("value");
            data.put(name, value);
        }

        return data;
    }

    private Map<String, String> sanitizeMap(Map<String, String> input) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        if (input == null) {
            return sanitized;
        }

        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }

        return sanitized;
    }

    private List<AdvancedSearchStep2Response.TabLayerData> extractTabLayers(Document document) {
        List<AdvancedSearchStep2Response.TabLayerData> layers = new ArrayList<>();
        Elements nodes = document.select("#tabNav > div.ongletLayer");

        for (Element node : nodes) {
            String id = cleanText(node.id());
            String className = cleanText(node.className());
            String text = cleanText(node.text());
            String html = node.outerHtml();

            List<String> links = new ArrayList<>();
            for (Element anchor : node.select("a[href]")) {
                String href = cleanText(anchor.absUrl("href"));
                if (href == null) {
                    href = cleanText(anchor.attr("href"));
                }
                if (href != null) {
                    links.add(href);
                }
            }

            List<List<String>> tableRows = new ArrayList<>();
            for (Element row : node.select("table tr")) {
                List<String> cells = new ArrayList<>();
                for (Element cell : row.select("th, td")) {
                    String cellText = cleanText(cell.text());
                    cells.add(cellText == null ? "" : cellText);
                }

                if (!cells.isEmpty()) {
                    tableRows.add(cells);
                }
            }

            layers.add(new AdvancedSearchStep2Response.TabLayerData(
                    id,
                    className,
                    text,
                    html,
                    links,
                    tableRows
            ));
        }

        return layers;
    }

    private AdvancedSearchStep2Response.TableData extractPrimaryTable(
            Document document,
            String baseUrl) {
        Element table = findBestResultsTable(document);
        if (table == null) {
            return new AdvancedSearchStep2Response.TableData(new ArrayList<>(), new ArrayList<>());
        }

        List<String> headers = extractHeaders(table);

        List<AdvancedSearchStep2Response.TableRowData> rows = new ArrayList<>();
        for (Element row : table.select("tr")) {
            Elements dataCells = row.select("td");
            if (dataCells.isEmpty()) {
                continue;
            }

            if (isLikelyHeaderRow(dataCells)) {
                continue;
            }

            List<String> cells = new ArrayList<>();
            Map<String, String> byHeader = new LinkedHashMap<>();
            List<String> links = new ArrayList<>();
            boolean thirdColumnPopupExists = false;
            String thirdColumnPopupHtml = null;
            String thirdColumnPopupHref = null;
            boolean popupExists = false;
            String popupHtml = null;
            String popupHref = null;
            Integer popupColumnIndex = null;
            String popupResolvedUrl = null;
            Integer popupResponseStatus = null;
            String popupResponseStatusMessage = null;
            String popupDetailText = null;
            String popupDetailHtml = null;

            for (int index = 0; index < dataCells.size(); index++) {
                Element cell = dataCells.get(index);
                String value = cleanText(cell.text());
                String normalizedValue = value == null ? "" : value;
                cells.add(normalizedValue);

                if (index < headers.size()) {
                    String key = headers.get(index);
                    if (key != null && !key.isBlank()) {
                        byHeader.put(key, normalizedValue);
                    }
                }

                for (Element link : cell.select("a[href]")) {
                    String href = cleanText(link.absUrl("href"));
                    if (href == null) {
                        href = cleanText(link.attr("href"));
                    }
                    if (href != null) {
                        links.add(href);
                    }
                }

                if (!popupExists) {
                    Element popupAnchor = cell.selectFirst("a[href^=javascript:popUp]");
                    if (popupAnchor != null) {
                        popupExists = true;
                        popupColumnIndex = index + 1;
                        popupHref = cleanText(popupAnchor.attr("href"));

                        Element popupContainer = popupAnchor.closest("span");
                        popupHtml = popupContainer != null ? popupContainer.outerHtml() : popupAnchor.outerHtml();
                    }
                }
            }

            if (dataCells.size() >= 3) {
                Element thirdColumn = dataCells.get(2);
                Element popupAnchor = thirdColumn.selectFirst("a[href^=javascript:popUp]");
                if (popupAnchor != null) {
                    thirdColumnPopupExists = true;
                    String thirdPopupHref = cleanText(popupAnchor.attr("href"));
                    thirdColumnPopupHref = thirdPopupHref == null ? "" : thirdPopupHref;

                    Element popupContainer = popupAnchor.closest("span");
                    if (popupContainer != null) {
                        thirdColumnPopupHtml = popupContainer.outerHtml();
                    } else {
                        thirdColumnPopupHtml = popupAnchor.outerHtml();
                    }
                }
            }

            if (!thirdColumnPopupExists && popupExists && popupColumnIndex != null && popupColumnIndex == 3) {
                thirdColumnPopupExists = true;
                thirdColumnPopupHref = popupHref;
                thirdColumnPopupHtml = popupHtml;
            }

            if (popupExists && popupHref != null) {
                popupResolvedUrl = resolvePopupUrl(baseUrl, popupHref);
            }

            rows.add(new AdvancedSearchStep2Response.TableRowData(
                    cells,
                    byHeader,
                    links,
                    thirdColumnPopupExists,
                    thirdColumnPopupHtml,
                    thirdColumnPopupHref,
                    popupExists,
                    popupHtml,
                    popupHref,
                    popupColumnIndex,
                    popupResolvedUrl,
                    popupResponseStatus,
                    popupResponseStatusMessage,
                    popupDetailText,
                    popupDetailHtml
            ));
        }

        if (headers.isEmpty() && !rows.isEmpty()) {
            int maxColumns = rows.stream().mapToInt(r -> r.getCells().size()).max().orElse(0);
            headers = new ArrayList<>();
            for (int i = 0; i < maxColumns; i++) {
                headers.add("column_" + (i + 1));
            }

            for (AdvancedSearchStep2Response.TableRowData row : rows) {
                Map<String, String> mapped = new LinkedHashMap<>();
                for (int i = 0; i < row.getCells().size() && i < headers.size(); i++) {
                    mapped.put(headers.get(i), row.getCells().get(i));
                }
                row.setByHeader(mapped);
            }
        }

        return new AdvancedSearchStep2Response.TableData(headers, rows);
    }

    private void enrichRowsWithPopupDetails(
            AdvancedSearchStep2Response.TableData table,
            Map<String, String> sessionCookies) {
        if (table == null || table.getRows() == null || table.getRows().isEmpty()) {
            return;
        }

        for (AdvancedSearchStep2Response.TableRowData row : table.getRows()) {
            if (row == null || row.getPopupResolvedUrl() == null || row.getPopupResolvedUrl().isBlank()) {
                continue;
            }

            PopupFetchResult popupFetchResult = fetchPopupContent(row.getPopupResolvedUrl(), sessionCookies);
            row.setPopupResponseStatus(popupFetchResult.statusCode());
            row.setPopupResponseStatusMessage(popupFetchResult.statusMessage());
            row.setPopupDetailText(popupFetchResult.text());
            row.setPopupDetailHtml(popupFetchResult.html());
        }
    }

    private String resolvePopupUrl(String baseUrl, String popupHref) {
        Matcher matcher = POPUP_JS_PATTERN.matcher(popupHref);
        if (!matcher.matches()) {
            return null;
        }

        String relative = matcher.group(1);
        if (relative == null || relative.isBlank()) {
            return null;
        }

        String decoded = relative.replace("&amp;", "&");
        return URI.create(baseUrl).resolve(decoded).toString();
    }

    private PopupFetchResult fetchPopupContent(String popupUrl, Map<String, String> sessionCookies) {
        try {
            Connection.Response popupResponse = Jsoup.connect(popupUrl)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(timeoutMs)
                    .method(Connection.Method.GET)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookies(sanitizeMap(sessionCookies))
                    .execute();

            Document popupDocument = Jsoup.parse(popupResponse.body(), popupResponse.url().toString());
            return new PopupFetchResult(
                    popupResponse.statusCode(),
                    popupResponse.statusMessage(),
                    cleanText(popupDocument.text()),
                    popupResponse.body()
            );
        } catch (IOException exception) {
            return new PopupFetchResult(0, "FETCH_ERROR", cleanText(exception.getMessage()), null);
        }
    }

    private record PopupFetchResult(int statusCode, String statusMessage, String text, String html) {
    }

    private List<String> extractHeaders(Element table) {
        List<String> headers = new ArrayList<>();

        Element headerRow = table.selectFirst("tr:has(th)");
        if (headerRow != null) {
            for (Element th : headerRow.select("th")) {
                String headerText = cleanText(th.text());
                headers.add(headerText == null ? "" : headerText);
            }
            headers = trimEdgeEmptyHeaders(headers);
            if (!headers.isEmpty()) {
                return headers;
            }
        }

        for (Element row : table.select("tr")) {
            Elements cells = row.select("th, td");
            if (cells.isEmpty()) {
                continue;
            }

            if (!isLikelyHeaderRow(cells)) {
                continue;
            }

            List<String> candidate = new ArrayList<>();
            for (Element cell : cells) {
                String text = cleanText(cell.text());
                candidate.add(text == null ? "" : text);
            }
            candidate = trimEdgeEmptyHeaders(candidate);
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }

        return headers;
    }

    private boolean isLikelyHeaderRow(Elements cells) {
        if (cells.isEmpty()) {
            return false;
        }

        String joined = normalize(cells.text());
        boolean hasProcedure = joined.contains("procedure") || joined.contains("categorie");
        boolean hasReference = joined.contains("reference") || joined.contains("programmme");
        boolean hasDateLimit = joined.contains("date limite") || joined.contains("remise des plis");
        boolean hasActions = joined.contains("actions");

        return hasProcedure && hasReference && hasDateLimit && hasActions;
    }

    private List<String> trimEdgeEmptyHeaders(List<String> headers) {
        int start = 0;
        int end = headers.size();

        while (start < end && (headers.get(start) == null || headers.get(start).isBlank())) {
            start++;
        }

        while (end > start && (headers.get(end - 1) == null || headers.get(end - 1).isBlank())) {
            end--;
        }

        return new ArrayList<>(headers.subList(start, end));
    }

    private Element findBestResultsTable(Document document) {
        Elements tables = document.select("#tabNav > div.ongletLayer table");
        Element bestTable = null;
        int bestScore = Integer.MIN_VALUE;

        for (Element table : tables) {
            int score = scoreTable(table);
            if (score > bestScore) {
                bestScore = score;
                bestTable = table;
            }
        }

        return bestTable;
    }

    private int scoreTable(Element table) {
        int score = 0;

        Element headerRow = table.selectFirst("tr:has(th)");
        if (headerRow != null) {
            Elements headerCells = headerRow.select("th");
            score += headerCells.size() * 5;
            String headerText = normalize(headerRow.text());
            if (headerText.contains("reference") || headerText.contains("objet") || headerText.contains("date limite") || headerText.contains("actions")) {
                score += 30;
            }
        }

        Element firstDataRow = table.selectFirst("tr:has(td)");
        if (firstDataRow != null) {
            Elements dataCells = firstDataRow.select("td");
            score += dataCells.size() * 2;
            if (dataCells.size() >= 5) {
                score += 20;
            }
            if (normalize(firstDataRow.text()).contains("acheteur public")) {
                score += 15;
            }
        }

        score += table.select("a[href^=javascript:popUp]").size() * 3;
        return score;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('ù', 'u')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}