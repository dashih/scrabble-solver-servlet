package org.dannyshih.scrabblesolver.dto;

public class VersionsResponse {
    private String app;
    private String springBoot;
    private String tomcat;
    private String java;
    private int numCores;
    private int maxConcurrentOperations;

    public VersionsResponse() {}

    public VersionsResponse(String app, String springBoot, String tomcat, String java, int numCores, int maxConcurrentOperations) {
        this.app = app;
        this.springBoot = springBoot;
        this.tomcat = tomcat;
        this.java = java;
        this.numCores = numCores;
        this.maxConcurrentOperations = maxConcurrentOperations;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getSpringBoot() {
        return springBoot;
    }

    public void setSpringBoot(String springBoot) {
        this.springBoot = springBoot;
    }

    public String getTomcat() {
        return tomcat;
    }

    public void setTomcat(String tomcat) {
        this.tomcat = tomcat;
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

    public int getMaxConcurrentOperations() {
        return maxConcurrentOperations;
    }

    public void setMaxConcurrentOperations(int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }
} 