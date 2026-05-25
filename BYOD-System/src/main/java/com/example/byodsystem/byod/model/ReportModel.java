package com.example.byodsystem.byod.model;

public class ReportModel {
    private int reportId;
    private String title;
    private String type;
    private String generatedBy;
    private String dateRange;
    private int totalRecords;
    private String createdAt;

    public ReportModel(int reportId, String title, String type, String generatedBy, String dateRange, int totalRecords, String createdAt) {
        this.reportId = reportId;
        this.title = title;
        this.type = type;
        this.generatedBy = generatedBy;
        this.dateRange = dateRange;
        this.totalRecords = totalRecords;
        this.createdAt = createdAt;
    }

    public int getReportId() {
        return reportId;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public String getDateRange() {
        return dateRange;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
