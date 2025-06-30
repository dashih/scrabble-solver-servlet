package org.dannyshih.scrabblesolver.dto;

public class VersionsResponse {
    private String app;
    private String server;
    private String springBoot;
    private String java;
    private int numCores;

    public VersionsResponse() {}

    public VersionsResponse(String app, String server, String springBoot, String java, int numCores) {
        this.app = app;
        this.server = server;
        this.springBoot = springBoot;
        this.java = java;
        this.numCores = numCores;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSpringBoot() {
        return springBoot;
    }

    public void setSpringBoot(String springBoot) {
        this.springBoot = springBoot;
    }

    public String getJava() {
        return java;
    }

    public void setJava(String java) {
        this.java = java;
    }

    public int getNumCores() {
        return numCores;
    }

    public void setNumCores(int numCores) {
        this.numCores = numCores;
    }
} 