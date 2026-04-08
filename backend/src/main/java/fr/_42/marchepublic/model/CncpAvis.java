package fr._42.marchepublic.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cncp_avis")
public class CncpAvis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_avis", unique = true)
    private String numeroAvis;

    @Column(name = "date_avis")
    private String date;

    @Column(name = "objet", columnDefinition = "TEXT")
    private String objet;

    @Column(name = "document_url")
    private String documentUrl;
}
