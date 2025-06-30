package org.dannyshih.scrabblesolver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scrabble.solver")
public class ScrabbleSolverConfig {
    
    private int maxConcurrentOperations;
    private int permutationBatchThreshold;

    public int getMaxConcurrentOperations() {
        return maxConcurrentOperations;
    }

    public void setMaxConcurrentOperations(int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }

    public int getPermutationBatchThreshold() {
        return permutationBatchThreshold;
    }

    public void setPermutationBatchThreshold(int permutationBatchThreshold) {
        this.permutationBatchThreshold = permutationBatchThreshold;
    }

    @Override
    public String toString() {
        return "ScrabbleSolverConfig{" +
                "maxConcurrentOperations=" + maxConcurrentOperations +
                ", permutationBatchThreshold=" + permutationBatchThreshold +
                '}';
    }
} 