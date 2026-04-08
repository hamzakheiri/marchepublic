package fr._42.marchepublic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lots")
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column
    private Integer lotNumber;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String estimation;

    @Column
    private String cautionProvisoire;

    @Column
    private String qualifications;

    @Column
    private String agrements;

    @Column(columnDefinition = "TEXT")
    private String visitesLieux;

    @Column
    private String variante;

    @Column(columnDefinition = "TEXT")
    private String considerationsEnv;

    @Column
    private String reserveTpePme;
}
