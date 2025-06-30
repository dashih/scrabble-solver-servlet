package org.dannyshih.scrabblesolver.dto;

import org.dannyshih.scrabblesolver.Progress;

public class GetProgressResponse {
    private SolveRequest params;
    private Progress.SerializableProgress progress;

    public GetProgressResponse() {}

    public GetProgressResponse(SolveRequest params, Progress.SerializableProgress progress) {
        this.params = params;
        this.progress = progress;
    }

    public SolveRequest getParams() {
        return params;
    }

    public void setParams(SolveRequest params) {
        this.params = params;
    }

    public Progress.SerializableProgress getProgress() {
        return progress;
    }

    public void setProgress(Progress.SerializableProgress progress) {
        this.progress = progress;
    }
} 