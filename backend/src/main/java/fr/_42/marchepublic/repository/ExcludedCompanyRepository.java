package fr._42.marchepublic.repository;

import fr._42.marchepublic.model.ExcludedCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcludedCompanyRepository extends JpaRepository<ExcludedCompany, Long> {
    boolean existsByDocumentId(Integer documentId);
}
