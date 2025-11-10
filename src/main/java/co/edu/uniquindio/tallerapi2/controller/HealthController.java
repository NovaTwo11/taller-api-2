package co.edu.uniquindio.tallerapi2.controller;

import co.edu.uniquindio.tallerapi2.infrastructure.AppInfo;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.Duration;
import java.util.*;

@RestController
public class HealthController {

    private final AppInfo appInfo;
    private final String version;

    public HealthController(AppInfo appInfo, @org.springframework.beans.factory.annotation.Value("${app.version:0.0.1}") String version) {
        this.appInfo = appInfo;
        this.version = version;
    }

    private Map<String,Object> buildHealth(String status, String detailStatusName) {
        Instant from = appInfo.getStartedAt();
        Map<String,Object> alive = Map.of(
                "data", Map.of("from", from.toString(), "status", detailStatusName),
                "name", detailStatusName.equals("READY") ? "Readiness check" : "Liveness check",
                "status", "UP"
        );

        Map<String,Object> root = new LinkedHashMap<>();
        root.put("status", status);
        List<Map<String,Object>> checks = new ArrayList<>();
        checks.add(alive);

        Map<String,Object> metadata = Map.of("version", version, "uptimeSeconds", Duration.between(from, Instant.now()).getSeconds());
        root.put("checks", checks);
        root.put("metadata", metadata);
        return root;
    }

    @GetMapping("/health")
    public Map<String,Object> health() {
        return buildHealth("UP", "READY");
    }

    @GetMapping("/health/ready")
    public Map<String,Object> readiness() {
        return buildHealth("UP", "READY");
    }

    @GetMapping("/health/live")
    public Map<String,Object> liveness() {
        return buildHealth("UP", "ALIVE");
    }
}
