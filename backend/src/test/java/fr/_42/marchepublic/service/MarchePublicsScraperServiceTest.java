package fr._42.marchepublic.service;

import com.sun.net.httpserver.HttpServer;
import fr._42.marchepublic.controller.dto.MarcheNoticeResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarchePublicsScraperServiceTest {

    @Test
    void scrapeParsesTableRowsAndExtractsDates() throws IOException {
        String html = """
                <html><body>
                  <table>
                    <tr><th>Title</th><th>Ref</th><th>Authority</th></tr>
                    <tr>
                      <td><a href=\"/notice/42\">AO Construction School</a></td>
                      <td>REF-2026-001</td>
                      <td>Ministry</td>
                      <td>Published 01/03/2026 closes 15/03/2026</td>
                    </tr>
                  </table>
                </body></html>
                """;

        try (TestPageServer server = TestPageServer.start(html)) {
            MarchePublicsScraperService service = new MarchePublicsScraperService(server.baseUrl(), 5000);

            List<MarcheNoticeResponse> notices = service.scrape(server.baseUrl(), 10);

            assertEquals(1, notices.size());
            MarcheNoticeResponse notice = notices.get(0);
            assertEquals("AO Construction School", notice.getTitle());
            assertEquals(server.baseUrl() + "/notice/42", notice.getUrl());
            assertEquals("REF-2026-001", notice.getReference());
            assertEquals("Ministry", notice.getAuthority());
            assertEquals("01/03/2026", notice.getPublicationDate());
            assertEquals("15/03/2026", notice.getDeadline());
        }
    }

    @Test
    void scrapeFallsBackToAnchorsWhenNoTableRows() throws IOException {
        String html = """
                <html><body>
                  <div>
                    <a href=\"/index.php?option=notice&id=7\">Tender notice for road maintenance in Casablanca</a>
                    <a href=\"https://example.com/other\">Should be ignored external link</a>
                  </div>
                </body></html>
                """;

        try (TestPageServer server = TestPageServer.start(html)) {
            MarchePublicsScraperService service = new MarchePublicsScraperService(server.baseUrl(), 5000);

            List<MarcheNoticeResponse> notices = service.scrape(server.baseUrl(), 10);

            assertEquals(1, notices.size());
            assertEquals("Tender notice for road maintenance in Casablanca", notices.get(0).getTitle());
            assertNotNull(notices.get(0).getUrl());
        }
    }

    @Test
    void scrapeHonorsMinimumLimit() throws IOException {
        String html = """
                <html><body>
                  <table>
                    <tr><td><a href=\"/a\">Very long title for item one</a></td><td>R1</td><td>A1</td></tr>
                    <tr><td><a href=\"/b\">Very long title for item two</a></td><td>R2</td><td>A2</td></tr>
                  </table>
                </body></html>
                """;

        try (TestPageServer server = TestPageServer.start(html)) {
            MarchePublicsScraperService service = new MarchePublicsScraperService(server.baseUrl(), 5000);

            List<MarcheNoticeResponse> notices = service.scrape(server.baseUrl(), 0);

            assertEquals(1, notices.size());
        }
    }

    @Test
    void scrapeRejectsInvalidScheme() {
        MarchePublicsScraperService service = new MarchePublicsScraperService("https://www.marchespublics.gov.ma", 5000);

        assertThrows(IllegalArgumentException.class, () -> service.scrape("ftp://bad.example", 5));
    }

    private static final class TestPageServer implements AutoCloseable {
        private final HttpServer server;
        private final String baseUrl;

        private TestPageServer(HttpServer server, String baseUrl) {
            this.server = server;
            this.baseUrl = baseUrl;
        }

        static TestPageServer start(String html) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", exchange -> {
                byte[] body = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
            });
            server.start();

            int port = server.getAddress().getPort();
            return new TestPageServer(server, "http://localhost:" + port);
        }

        String baseUrl() {
            return baseUrl;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
