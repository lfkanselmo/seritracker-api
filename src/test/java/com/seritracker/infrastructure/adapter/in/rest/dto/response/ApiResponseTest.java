package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse")
class ApiResponseTest {

    @Test
    @DisplayName("ok() should return success response with data")
    void ok_shouldReturnSuccessResponse() {
        ApiResponse<String> response = ApiResponse.ok("test data");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test data");
        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("created() should return success response with 'Created' message")
    void created_shouldReturnCreatedResponse() {
        ApiResponse<String> response = ApiResponse.created("new resource");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("new resource");
        assertThat(response.getMessage()).isEqualTo("Created");
    }

    @Test
    @DisplayName("noContent() should return success response with null data and 'Deleted' message")
    void noContent_shouldReturnNoContentResponse() {
        ApiResponse<Void> response = ApiResponse.noContent();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("Deleted");
    }

    @Test
    @DisplayName("noContent(message) should return success response with the given message")
    void noContentWithMessage_shouldReturnGivenMessage() {
        ApiResponse<Void> response = ApiResponse.noContent("Password changed");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("Password changed");
    }
}