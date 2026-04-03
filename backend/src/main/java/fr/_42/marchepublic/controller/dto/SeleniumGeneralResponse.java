package fr._42.marchepublic.controller.dto;

import java.util.List;
import java.util.Map;

public class SeleniumGeneralResponse {
    private String sourceUrl;
    private String pageUrl;
    private List<String> headers;
    private List<OfferRow> rows;

    public SeleniumGeneralResponse() {
    }

    public SeleniumGeneralResponse(String sourceUrl, String pageUrl, List<String> headers, List<OfferRow> rows) {
        this.sourceUrl = sourceUrl;
        this.pageUrl = pageUrl;
        this.headers = headers;
        this.rows = rows;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<OfferRow> getRows() {
        return rows;
    }

    public void setRows(List<OfferRow> rows) {
        this.rows = rows;
    }

    public static class OfferRow {
        private List<String> cells;
        private Map<String, String> byHeader;
        private List<String> links;
        private String popupHref;
        private String popupResolvedUrl;

        public OfferRow() {
        }

        public OfferRow(List<String> cells, Map<String, String> byHeader, List<String> links, String popupHref, String popupResolvedUrl) {
            this.cells = cells;
            this.byHeader = byHeader;
            this.links = links;
            this.popupHref = popupHref;
            this.popupResolvedUrl = popupResolvedUrl;
        }

        public List<String> getCells() {
            return cells;
        }

        public void setCells(List<String> cells) {
            this.cells = cells;
        }

        public Map<String, String> getByHeader() {
            return byHeader;
        }

        public void setByHeader(Map<String, String> byHeader) {
            this.byHeader = byHeader;
        }

        public List<String> getLinks() {
            return links;
        }

        public void setLinks(List<String> links) {
            this.links = links;
        }

        public String getPopupHref() {
            return popupHref;
        }

        public void setPopupHref(String popupHref) {
            this.popupHref = popupHref;
        }

        public String getPopupResolvedUrl() {
            return popupResolvedUrl;
        }

        public void setPopupResolvedUrl(String popupResolvedUrl) {
            this.popupResolvedUrl = popupResolvedUrl;
        }
    }
}
