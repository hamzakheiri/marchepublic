package fr._42.marchepublic.controller.dto;

public record ExcludedCompany(
        String entitePublique,
        String registreCommerce,
        String raisonSociale,
        String motif,
        String dateDebut,
        String dateFin,
        String portee,
        String documentUrl,
        String documentInfo
) {}
