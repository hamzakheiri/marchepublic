package fr._42.marchepublic.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "excluded_companies")
public class ExcludedCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique identifier extracted from the document popup URL (id=N)
    @Column(unique = true, nullable = false)
    private Integer documentId;

    @Column(columnDefinition = "TEXT")
    private String entitePublique;

    @Column
    private String registreCommerce;

    @Column(columnDefinition = "TEXT")
    private String raisonSociale;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column
    private String dateDebut;

    @Column
    private String dateFin;

    @Column
    private String portee;

    @Column(columnDefinition = "TEXT")
    private String documentUrl;

    @Column
    private String documentInfo;
}
