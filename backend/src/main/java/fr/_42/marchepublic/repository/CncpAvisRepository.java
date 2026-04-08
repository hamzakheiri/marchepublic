package fr._42.marchepublic.repository;

import fr._42.marchepublic.model.CncpAvis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CncpAvisRepository extends JpaRepository<CncpAvis, Long> {
    boolean existsByNumeroAvis(String numeroAvis);
}
