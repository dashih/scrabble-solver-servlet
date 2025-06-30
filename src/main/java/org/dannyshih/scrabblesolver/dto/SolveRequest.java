package org.dannyshih.scrabblesolver.dto;

public class SolveRequest {
    private boolean parallelMode;
    private String input;
    private String regex;
    private int minChars;

    // Default constructor for JSON deserialization
    public SolveRequest() {}

    public SolveRequest(boolean parallelMode, String input, String regex, int minChars) {
        this.parallelMode = parallelMode;
        this.input = input;
        this.regex = regex;
        this.minChars = minChars;
    }

    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public int getMinChars() {
        return minChars;
    }

    public void setMinChars(int minChars) {
        this.minChars = minChars;
    }
} 