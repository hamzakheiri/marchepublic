package fr._42.marchepublic.controller;

import fr._42.marchepublic.model.ConsultationDocument;
import fr._42.marchepublic.repository.ConsultationDocumentRepository;
import fr._42.marchepublic.service.TestScraperService;
import org.jsoup.Connection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class ConsultationDocumentController {

    private final ConsultationDocumentRepository consultationDocumentRepository;
    private final TestScraperService testScraperService;

    public ConsultationDocumentController(
            ConsultationDocumentRepository consultationDocumentRepository,
            TestScraperService testScraperService) {
        this.consultationDocumentRepository = consultationDocumentRepository;
        this.testScraperService = testScraperService;
    }

    @GetMapping
    public List<ConsultationDocument> listAll() {
        return consultationDocumentRepository.findAll();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ConsultationDocument doc = consultationDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        Connection.Response response = testScraperService.fetchBytesWithCookies(doc.getUrl());

        String contentType = response.contentType();
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        String filename = doc.getLabel() != null ? doc.getLabel() : "document-" + id;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(response.bodyAsBytes());
    }
}
