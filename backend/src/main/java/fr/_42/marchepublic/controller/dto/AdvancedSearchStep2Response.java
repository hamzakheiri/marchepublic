package fr._42.marchepublic.controller.dto;

import java.util.Map;
import java.util.List;

public class AdvancedSearchStep2Response {
    private String initialUrl;
    private String formAction;
    private String method;
    private int responseStatus;
    private String responseStatusMessage;
    private String responseUrl;
    private String pageTitle;
    private boolean launchSearchButtonUsed;
    private int requestFieldCount;
    private Map<String, String> cookies;
    private List<TabLayerData> tabLayers;
    private TableData table;
    private String responseHtml;

    public AdvancedSearchStep2Response() {
    }

    public AdvancedSearchStep2Response(
            String initialUrl,
            String formAction,
            String method,
            int responseStatus,
            String responseStatusMessage,
            String responseUrl,
            String pageTitle,
            boolean launchSearchButtonUsed,
            int requestFieldCount,
            Map<String, String> cookies,
            List<TabLayerData> tabLayers,
            TableData table,
            String responseHtml) {
        this.initialUrl = initialUrl;
        this.formAction = formAction;
        this.method = method;
        this.responseStatus = responseStatus;
        this.responseStatusMessage = responseStatusMessage;
        this.responseUrl = responseUrl;
        this.pageTitle = pageTitle;
        this.launchSearchButtonUsed = launchSearchButtonUsed;
        this.requestFieldCount = requestFieldCount;
        this.cookies = cookies;
        this.tabLayers = tabLayers;
        this.table = table;
        this.responseHtml = responseHtml;
    }

    public String getInitialUrl() {
        return initialUrl;
    }

    public void setInitialUrl(String initialUrl) {
        this.initialUrl = initialUrl;
    }

    public String getFormAction() {
        return formAction;
    }

    public void setFormAction(String formAction) {
        this.formAction = formAction;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseStatusMessage() {
        return responseStatusMessage;
    }

    public void setResponseStatusMessage(String responseStatusMessage) {
        this.responseStatusMessage = responseStatusMessage;
    }

    public String getResponseUrl() {
        return responseUrl;
    }

    public void setResponseUrl(String responseUrl) {
        this.responseUrl = responseUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public boolean isLaunchSearchButtonUsed() {
        return launchSearchButtonUsed;
    }

    public void setLaunchSearchButtonUsed(boolean launchSearchButtonUsed) {
        this.launchSearchButtonUsed = launchSearchButtonUsed;
    }

    public int getRequestFieldCount() {
        return requestFieldCount;
    }

    public void setRequestFieldCount(int requestFieldCount) {
        this.requestFieldCount = requestFieldCount;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public List<TabLayerData> getTabLayers() {
        return tabLayers;
    }

    public void setTabLayers(List<TabLayerData> tabLayers) {
        this.tabLayers = tabLayers;
    }

    public TableData getTable() {
        return table;
    }

    public void setTable(TableData table) {
        this.table = table;
    }

    public String getResponseHtml() {
        return responseHtml;
    }

    public void setResponseHtml(String responseHtml) {
        this.responseHtml = responseHtml;
    }

    public static class TabLayerData {
        private String id;
        private String className;
        private String text;
        private String html;
        private List<String> links;
        private List<List<String>> tableRows;

        public TabLayerData() {
        }

        public TabLayerData(
                String id,
                String className,
                String text,
                String html,
                List<String> links,
                List<List<String>> tableRows) {
            this.id = id;
            this.className = className;
            this.text = text;
            this.html = html;
            this.links = links;
            this.tableRows = tableRows;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getHtml() {
            return html;
        }

        public void setHtml(String html) {
            this.html = html;
        }

        public List<String> getLinks() {
            return links;
        }

        public void setLinks(List<String> links) {
            this.links = links;
        }

        public List<List<String>> getTableRows() {
            return tableRows;
        }

        public void setTableRows(List<List<String>> tableRows) {
            this.tableRows = tableRows;
        }
    }

    public static class TableData {
        private List<String> headers;
        private List<TableRowData> rows;

        public TableData() {
        }

        public TableData(List<String> headers, List<TableRowData> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public void setHeaders(List<String> headers) {
            this.headers = headers;
        }

        public List<TableRowData> getRows() {
            return rows;
        }

        public void setRows(List<TableRowData> rows) {
            this.rows = rows;
        }
    }

    public static class TableRowData {
        private List<String> cells;
        private Map<String, String> byHeader;
        private List<String> links;
        private boolean thirdColumnPopupExists;
        private String thirdColumnPopupHtml;
        private String thirdColumnPopupHref;
        private boolean popupExists;
        private String popupHtml;
        private String popupHref;
        private Integer popupColumnIndex;
        private String popupResolvedUrl;
        private Integer popupResponseStatus;
        private String popupResponseStatusMessage;
        private String popupDetailText;
        private String popupDetailHtml;

        public TableRowData() {
        }

        public TableRowData(
                List<String> cells,
                Map<String, String> byHeader,
                List<String> links,
                boolean thirdColumnPopupExists,
                String thirdColumnPopupHtml,
                String thirdColumnPopupHref,
                boolean popupExists,
                String popupHtml,
                String popupHref,
                Integer popupColumnIndex) {
            this.cells = cells;
            this.byHeader = byHeader;
            this.links = links;
            this.thirdColumnPopupExists = thirdColumnPopupExists;
            this.thirdColumnPopupHtml = thirdColumnPopupHtml;
            this.thirdColumnPopupHref = thirdColumnPopupHref;
            this.popupExists = popupExists;
            this.popupHtml = popupHtml;
            this.popupHref = popupHref;
            this.popupColumnIndex = popupColumnIndex;
        }

        public TableRowData(
                List<String> cells,
                Map<String, String> byHeader,
                List<String> links,
                boolean thirdColumnPopupExists,
                String thirdColumnPopupHtml,
                String thirdColumnPopupHref,
                boolean popupExists,
                String popupHtml,
                String popupHref,
                Integer popupColumnIndex,
                String popupResolvedUrl,
                Integer popupResponseStatus,
                String popupResponseStatusMessage,
                String popupDetailText,
                String popupDetailHtml) {
            this.cells = cells;
            this.byHeader = byHeader;
            this.links = links;
            this.thirdColumnPopupExists = thirdColumnPopupExists;
            this.thirdColumnPopupHtml = thirdColumnPopupHtml;
            this.thirdColumnPopupHref = thirdColumnPopupHref;
            this.popupExists = popupExists;
            this.popupHtml = popupHtml;
            this.popupHref = popupHref;
            this.popupColumnIndex = popupColumnIndex;
            this.popupResolvedUrl = popupResolvedUrl;
            this.popupResponseStatus = popupResponseStatus;
            this.popupResponseStatusMessage = popupResponseStatusMessage;
            this.popupDetailText = popupDetailText;
            this.popupDetailHtml = popupDetailHtml;
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

        public boolean isThirdColumnPopupExists() {
            return thirdColumnPopupExists;
        }

        public void setThirdColumnPopupExists(boolean thirdColumnPopupExists) {
            this.thirdColumnPopupExists = thirdColumnPopupExists;
        }

        public String getThirdColumnPopupHtml() {
            return thirdColumnPopupHtml;
        }

        public void setThirdColumnPopupHtml(String thirdColumnPopupHtml) {
            this.thirdColumnPopupHtml = thirdColumnPopupHtml;
        }

        public String getThirdColumnPopupHref() {
            return thirdColumnPopupHref;
        }

        public void setThirdColumnPopupHref(String thirdColumnPopupHref) {
            this.thirdColumnPopupHref = thirdColumnPopupHref;
        }

        public boolean isPopupExists() {
            return popupExists;
        }

        public void setPopupExists(boolean popupExists) {
            this.popupExists = popupExists;
        }

        public String getPopupHtml() {
            return popupHtml;
        }

        public void setPopupHtml(String popupHtml) {
            this.popupHtml = popupHtml;
        }

        public String getPopupHref() {
            return popupHref;
        }

        public void setPopupHref(String popupHref) {
            this.popupHref = popupHref;
        }

        public Integer getPopupColumnIndex() {
            return popupColumnIndex;
        }

        public void setPopupColumnIndex(Integer popupColumnIndex) {
            this.popupColumnIndex = popupColumnIndex;
        }

        public String getPopupResolvedUrl() {
            return popupResolvedUrl;
        }

        public void setPopupResolvedUrl(String popupResolvedUrl) {
            this.popupResolvedUrl = popupResolvedUrl;
        }

        public Integer getPopupResponseStatus() {
            return popupResponseStatus;
        }

        public void setPopupResponseStatus(Integer popupResponseStatus) {
            this.popupResponseStatus = popupResponseStatus;
        }

        public String getPopupResponseStatusMessage() {
            return popupResponseStatusMessage;
        }

        public void setPopupResponseStatusMessage(String popupResponseStatusMessage) {
            this.popupResponseStatusMessage = popupResponseStatusMessage;
        }

        public String getPopupDetailText() {
            return popupDetailText;
        }

        public void setPopupDetailText(String popupDetailText) {
            this.popupDetailText = popupDetailText;
        }

        public String getPopupDetailHtml() {
            return popupDetailHtml;
        }

        public void setPopupDetailHtml(String popupDetailHtml) {
            this.popupDetailHtml = popupDetailHtml;
        }
    }
}