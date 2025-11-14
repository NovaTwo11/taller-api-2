package co.edu.uniquindio.tallerapi2.infrastructure;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Component
public class AppInfo {
    private Instant startedAt;

    @PostConstruct
    public void init() {
        startedAt = Instant.now();
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}