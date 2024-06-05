package demo.controller;

import demo.model.Worker;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClient;

@Controller
public class WorkerController {
    private String hostname;
    private Worker self;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWorkerOnStartup() {
        String appType = System.getenv().get("APP_TYPE");
        System.out.println("Application Type: " + appType);

        if (!"worker".equals(appType)) {
            System.out.println("This application is of type '" + appType + "' and is not configured as a worker.");
            return;
        }

        String hostname = System.getenv().get("HOSTNAME");
        System.out.println("Hostname: " + hostname);

        if (hostname == null) {
            System.out.println("Hostname is not set, worker cannot be registered.");
            return;
        }

        Worker self = new Worker(hostname);
        try {
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri("http://registery:8081/workers/manifest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(self)
                    .retrieve();
            System.out.println("Worker registered: " + self.getHostname());
        } catch (Exception e) {
            System.err.println("Failed to register worker '" + hostname + "': " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void registerWorker() {
        String appType = System.getenv().get("APP_TYPE");
        if (!"worker".equals(appType)) {
            System.out.println("This service is not configured as a worker and will not register.");
            return;
        }

        if (this.self == null || this.hostname == null) {
            System.out.println("Worker or hostname is not properly initialized; registration cannot proceed.");
            return;
        }

        System.out.println("Attempting to register worker '" + this.hostname + "'.");

        try {
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri("http://registery:8081/workers/manifest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(this.self)
                    .retrieve();
            System.out.println("Worker '" + this.hostname + "' successfully registered.");
        } catch (Exception e) {
            System.err.println("Failed to register worker '" + this.hostname + "': " + e.getMessage());
        }
    }

    @PostMapping("/hello")
    public ResponseEntity<String> hello(@RequestBody String name) {
        String appType = System.getenv().get("APP_TYPE");

        if (!"worker".equals(appType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("This service is not configured to handle greetings as it is not a worker.");
        }

        if (hostname == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hostname is undefined; cannot process the greeting.");
        }

        String response = "Hello " + name + ", I am " + hostname;

        System.out.println("Greeting processed for '" + name + "': " + response);

        return ResponseEntity.ok(response);
    }

}
