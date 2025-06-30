package org.dannyshih.scrabblesolver.dto;

import java.util.Map;
import java.util.UUID;

public class CurrentlyRunningResponse {
    private Map<UUID, String> operations;

    public CurrentlyRunningResponse() {}

    public CurrentlyRunningResponse(Map<UUID, String> operations) {
        this.operations = operations;
    }

    public Map<UUID, String> getOperations() {
        return operations;
    }

    public void setOperations(Map<UUID, String> operations) {
        this.operations = operations;
    }
} 