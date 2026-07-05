package com.seritracker.domain.model;

public enum SeriesSortBy {
    TITLE("title"),
    RATING("rating"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt");

    private final String fieldName;

    SeriesSortBy(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
