package fr._42.marchepublic.repository;

import fr._42.marchepublic.model.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {
    List<Lot> findByConsultationId(Long consultationId);
    boolean existsByConsultationIdAndLotNumber(Long consultationId, Integer lotNumber);
}
