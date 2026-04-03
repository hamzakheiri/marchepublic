package fr._42.marchepublic.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consultations")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String refConsultation;

    @Column
    private String orgAcronyme;

    @Column
    private String procedureType;

    @Column
    private String procedureFullName;

    @Column
    private String category;

    @Column
    private String publishedDate;

    @Column
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String object;

    @Column(columnDefinition = "TEXT")
    private String buyer;

    @Column
    private String location;

    @Column
    private String deadline;

    @Column(columnDefinition = "TEXT")
    private String detailUrl;

    @Column(columnDefinition = "TEXT")
    private String lotsPopupUrl;
}
