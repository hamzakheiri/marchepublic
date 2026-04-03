package fr._42.marchepublic.controller.dto;

public record ConsultationRow(
        String refConsultation,
        String orgAcronyme,
        String procedureType,
        String procedureFullName,
        String category,
        String publishedDate,
        String reference,
        String object,
        String buyer,
        String location,
        String deadline,
        String detailUrl
) {}
