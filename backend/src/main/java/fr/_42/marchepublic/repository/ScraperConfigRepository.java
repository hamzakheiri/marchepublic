package fr._42.marchepublic.repository;

import fr._42.marchepublic.model.ScraperConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScraperConfigRepository extends JpaRepository<ScraperConfig, Long> {
}
