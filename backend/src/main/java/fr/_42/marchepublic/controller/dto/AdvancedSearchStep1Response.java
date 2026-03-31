package fr._42.marchepublic.controller.dto;

public class AdvancedSearchStep1Response {
    private String sourceUrl;
    private String finalUrl;
    private String pageTitle;
    private String formAction;
    private boolean launchSearchButtonFound;
    private int formCount;

    public AdvancedSearchStep1Response() {
    }

    public AdvancedSearchStep1Response(
            String sourceUrl,
            String finalUrl,
            String pageTitle,
            String formAction,
            boolean launchSearchButtonFound,
            int formCount) {
        this.sourceUrl = sourceUrl;
        this.finalUrl = finalUrl;
        this.pageTitle = pageTitle;
        this.formAction = formAction;
        this.launchSearchButtonFound = launchSearchButtonFound;
        this.formCount = formCount;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getFormAction() {
        return formAction;
    }

    public void setFormAction(String formAction) {
        this.formAction = formAction;
    }

    public boolean isLaunchSearchButtonFound() {
        return launchSearchButtonFound;
    }

    public void setLaunchSearchButtonFound(boolean launchSearchButtonFound) {
        this.launchSearchButtonFound = launchSearchButtonFound;
    }

    public int getFormCount() {
        return formCount;
    }

    public void setFormCount(int formCount) {
        this.formCount = formCount;
    }
}