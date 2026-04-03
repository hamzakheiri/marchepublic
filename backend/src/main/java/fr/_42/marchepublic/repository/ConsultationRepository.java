package fr._42.marchepublic.repository;

import fr._42.marchepublic.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Optional<Consultation> findByRefConsultation(String refConsultation);
    boolean existsByRefConsultation(String refConsultation);
}
