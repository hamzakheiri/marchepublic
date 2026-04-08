package fr._42.marchepublic.controller;

import fr._42.marchepublic.model.Consultation;
import fr._42.marchepublic.model.ConsultationDocument;
import fr._42.marchepublic.model.Lot;
import fr._42.marchepublic.repository.ConsultationDocumentRepository;
import fr._42.marchepublic.repository.ConsultationRepository;
import fr._42.marchepublic.repository.LotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/consultations")
public class ConsultationController {

    private final ConsultationRepository consultationRepository;
    private final LotRepository lotRepository;
    private final ConsultationDocumentRepository consultationDocumentRepository;

    public ConsultationController(
            ConsultationRepository consultationRepository,
            LotRepository lotRepository,
            ConsultationDocumentRepository consultationDocumentRepository) {
        this.consultationRepository = consultationRepository;
        this.lotRepository = lotRepository;
        this.consultationDocumentRepository = consultationDocumentRepository;
    }

    @GetMapping
    public Page<Consultation> list(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return consultationRepository.findAll(pageable);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "totalConsultations", consultationRepository.count(),
                "totalLots", lotRepository.count(),
                "totalDocuments", consultationDocumentRepository.count()
        );
    }

    @GetMapping("/{id}/lots")
    public List<Lot> lots(@PathVariable Long id) {
        return lotRepository.findByConsultationId(id);
    }

    @GetMapping("/{id}/documents")
    public List<ConsultationDocument> documents(@PathVariable Long id) {
        return consultationDocumentRepository.findByConsultationId(id);
    }
}
