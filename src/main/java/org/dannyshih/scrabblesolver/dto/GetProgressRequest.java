package org.dannyshih.scrabblesolver.dto;

import java.util.UUID;

public class GetProgressRequest {
    private UUID id;

    public GetProgressRequest() {}

    public GetProgressRequest(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
} 