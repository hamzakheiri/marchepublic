package fr._42.marchepublic.repository;

import fr._42.marchepublic.model.ConsultationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationDocumentRepository extends JpaRepository<ConsultationDocument, Long> {
    List<ConsultationDocument> findByConsultationId(Long consultationId);
    boolean existsByConsultationIdAndType(Long consultationId, String type);
}
