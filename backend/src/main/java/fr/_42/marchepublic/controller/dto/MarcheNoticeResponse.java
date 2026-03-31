package fr._42.marchepublic.controller.dto;

public class MarcheNoticeResponse {
    private String title;
    private String url;
    private String reference;
    private String authority;
    private String publicationDate;
    private String deadline;

    public MarcheNoticeResponse() {
    }

    public MarcheNoticeResponse(
            String title,
            String url,
            String reference,
            String authority,
            String publicationDate,
            String deadline) {
        this.title = title;
        this.url = url;
        this.reference = reference;
        this.authority = authority;
        this.publicationDate = publicationDate;
        this.deadline = deadline;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }
}
