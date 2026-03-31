package fr._42.marchepublic.service;

import fr._42.marchepublic.controller.dto.MarcheNoticeResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarchePublicsScraperService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b");

    private final String defaultUrl;
    private final int timeoutMs;

    public MarchePublicsScraperService(
            @Value("${scraper.marches-publics.default-url}") String defaultUrl,
            @Value("${scraper.marches-publics.timeout-ms}") int timeoutMs) {
        this.defaultUrl = defaultUrl;
        this.timeoutMs = timeoutMs;
    }

    public List<MarcheNoticeResponse> scrape(String requestedUrl, int limit) {
        String url = sanitizeUrl(requestedUrl);
        int boundedLimit = Math.min(Math.max(limit, 1), 100);

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(timeoutMs)
                    .get();

            List<MarcheNoticeResponse> notices = parseTableRows(document, boundedLimit);
            if (!notices.isEmpty()) {
                return notices;
            }

            return parseAnchors(document, boundedLimit);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to fetch marchespublics.gov.ma page", exception);
        }
    }

    private List<MarcheNoticeResponse> parseTableRows(Document document, int limit) {
        List<MarcheNoticeResponse> results = new ArrayList<>();

        for (Element row : document.select("table tr")) {
            if (results.size() >= limit) {
                break;
            }

            if (!row.select("th").isEmpty()) {
                continue;
            }

            Elements cells = row.select("td");
            if (cells.isEmpty()) {
                continue;
            }

            Element anchor = row.selectFirst("a[href]");
            String title = firstNonBlank(
                    text(cells, 0),
                    anchor != null ? anchor.text() : null
            );

            if (title == null || title.length() < 4) {
                continue;
            }

            String rowText = row.text();
            List<String> dates = findDates(rowText);

            results.add(new MarcheNoticeResponse(
                    title,
                    anchor != null ? anchor.absUrl("href") : null,
                    text(cells, 1),
                    text(cells, 2),
                    dates.isEmpty() ? null : dates.get(0),
                    dates.size() > 1 ? dates.get(1) : null
            ));
        }

        return results;
    }

    private List<MarcheNoticeResponse> parseAnchors(Document document, int limit) {
        List<MarcheNoticeResponse> results = new ArrayList<>();

        for (Element anchor : document.select("a[href]")) {
            if (results.size() >= limit) {
                break;
            }

            String href = anchor.absUrl("href");
            String title = cleanText(anchor.text());

            if (href == null || href.isBlank() || title == null || title.length() < 12) {
                continue;
            }

            String lowerHref = href.toLowerCase(Locale.ROOT);
            if (!lowerHref.contains("marchespublics.gov.ma") && !lowerHref.contains("/index.php")) {
                continue;
            }

            results.add(new MarcheNoticeResponse(title, href, null, null, null, null));
        }

        return results;
    }

    private String sanitizeUrl(String requestedUrl) {
        if (requestedUrl == null || requestedUrl.isBlank()) {
            return defaultUrl;
        }

        String trimmed = requestedUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("url must start with http:// or https://");
        }

        return trimmed;
    }

    private String text(Elements cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }

        return cleanText(cells.get(index).text());
    }

    private List<String> findDates(String input) {
        List<String> dates = new ArrayList<>();
        Matcher matcher = DATE_PATTERN.matcher(input);

        while (matcher.find()) {
            dates.add(matcher.group());
        }

        return dates;
    }

    private String firstNonBlank(String first, String second) {
        String cleanedFirst = cleanText(first);
        if (cleanedFirst != null && !cleanedFirst.isBlank()) {
            return cleanedFirst;
        }

        String cleanedSecond = cleanText(second);
        if (cleanedSecond != null && !cleanedSecond.isBlank()) {
            return cleanedSecond;
        }

        return null;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
