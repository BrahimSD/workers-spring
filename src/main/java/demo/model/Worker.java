package demo.model;

import jakarta.persistence.*;

@Entity
public class Worker {
    @Id
    private String hostname;

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
}
