package org.dannyshih.scrabblesolver.controller;

import org.dannyshih.scrabblesolver.config.ScrabbleSolverConfig;
import org.dannyshih.scrabblesolver.dto.CurrentlyRunningResponse;
import org.dannyshih.scrabblesolver.dto.GetProgressRequest;
import org.dannyshih.scrabblesolver.dto.GetProgressResponse;
import org.dannyshih.scrabblesolver.dto.SolveRequest;
import org.dannyshih.scrabblesolver.dto.SolveResponse;
import org.dannyshih.scrabblesolver.dto.VersionsResponse;
import org.dannyshih.scrabblesolver.service.ScrabbleSolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ScrabbleSolverController {

    private final ScrabbleSolverService scrabbleSolverService;

    @Autowired
    public ScrabbleSolverController(ScrabbleSolverService scrabbleSolverService) {
        this.scrabbleSolverService = scrabbleSolverService;
    }

    @PostMapping("/solve")
    public ResponseEntity<SolveResponse> solve(@RequestBody SolveRequest solveRequest) {
        UUID operationId = scrabbleSolverService.startSolve(solveRequest);
        return ResponseEntity.ok(new SolveResponse(operationId));
    }

    @PostMapping("/getProgress")
    public ResponseEntity<?> getProgress(@RequestBody GetProgressRequest request) {
        ScrabbleSolverService.Operation op = scrabbleSolverService.getOperation(request.getId());
        
        if (op == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Operation not found");
        }

        if (op.progress.getRunStatus() == org.dannyshih.scrabblesolver.Progress.RunStatus.Canceled) {
            return ResponseEntity.status(HttpStatus.GONE).body("Operation canceled");
        }

        if (op.isCancellationRequested.get()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Cancellation pending");
        }

        if (op.progress.getRunStatus() == org.dannyshih.scrabblesolver.Progress.RunStatus.Failed) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(op.progress.getError().getMessage());
        }

        GetProgressResponse response = new GetProgressResponse(op.params, op.progress.toSerializable());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestBody GetProgressRequest request) {
        boolean canceled = scrabbleSolverService.cancelOperation(request.getId());
        if (canceled) {
            return ResponseEntity.ok("Cancellation requested");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Operation not found");
        }
    }

    @PostMapping("/getVersions")
    public ResponseEntity<VersionsResponse> getVersions() {
        String appVersion = scrabbleSolverService.getAppVersion();
        String springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();
        String tomcatVersion = org.apache.catalina.util.ServerInfo.getServerInfo();
        String javaVersion = System.getProperty("java.version");
        int numCores = Runtime.getRuntime().availableProcessors();

        VersionsResponse response = new VersionsResponse(
                appVersion,
                springBootVersion,
                tomcatVersion,
                javaVersion,
                numCores);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/getCurrentlyRunning")
    public ResponseEntity<CurrentlyRunningResponse> getCurrentlyRunning() {
        var operations = scrabbleSolverService.getCurrentlyRunningOperations();
        CurrentlyRunningResponse response = new CurrentlyRunningResponse(operations);
        return ResponseEntity.ok(response);
    }
} 