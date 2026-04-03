package fr._42.marchepublic.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class TestScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final String baseUrl;
    private final int timeoutMs;

    public TestScraperService(
            @Value("${scraper.marches-publics.default-url}") String baseUrl,
            @Value("${scraper.marches-publics.timeout-ms}") int timeoutMs) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
    }

    public String fetchWithCookies(String targetUrl) {
        try {
            Connection.Response baseResponse = Jsoup.connect(baseUrl)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .method(Connection.Method.GET)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .execute();

            Map<String, String> cookies = baseResponse.cookies();

            Connection.Response popupResponse = Jsoup.connect(targetUrl)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .method(Connection.Method.GET)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .cookies(cookies)
                    .execute();

            return popupResponse.body();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch popup URL: " + e.getMessage(), e);
        }
    }

    public Connection.Response fetchBytesWithCookies(String targetUrl) {
        try {
            Connection.Response baseResponse = Jsoup.connect(baseUrl)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .method(Connection.Method.GET)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .execute();

            Map<String, String> cookies = baseResponse.cookies();

            return Jsoup.connect(targetUrl)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .method(Connection.Method.GET)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .cookies(cookies)
                    .execute();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch file: " + e.getMessage(), e);
        }
    }
}
