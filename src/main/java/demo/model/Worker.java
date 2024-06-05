package demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
public class Worker {
    @Id
    private String hostname;
    private LocalDateTime lastManfTime;

    private String service;


    public Worker() {
    }
    public Worker(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public LocalDateTime getLastManifestTime() {
        return lastManfTime;
    }

    public void setLastManifestTime(LocalDateTime lastManfTime) {
        this.lastManfTime = lastManfTime;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}
