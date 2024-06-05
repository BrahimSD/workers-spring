package demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.model.Worker;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@CrossOrigin

@Controller
public class LoadBalancer {
    private List<Worker> workers;

    private int index = 0;

    @GetMapping("/service/hello/{name}")
    public ResponseEntity<String> hello(@PathVariable String name) {
        if (!"loadbalancer".equals(System.getenv("APP_TYPE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid application type");
        }

        if (workers == null || workers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No workers available");
        }

        Worker worker = workers.get(new Random().nextInt(workers.size()));

        System.out.println("Request forwarded to " + worker.getHostname());

        RestClient restClient = RestClient.create();
        String result;
        try {
            result = restClient.post()
                    .uri("http://" + worker.getHostname() + ":8081/hello")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(name)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            System.err.println("Error sending request to " + worker.getHostname() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process the request");
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/postworkers")
    public ResponseEntity<String> postWorkers(@RequestBody List<Worker> newWorkers) {
        if (!"loadbalancer".equals(System.getenv("APP_TYPE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Service is not configured as a load balancer.");
        }
        if (newWorkers == null || newWorkers.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid worker list: Cannot be null or empty.");
        }
        this.workers = newWorkers;
        System.out.println("Workers list successfully updated with " + newWorkers.size() + " workers.");

        return ResponseEntity.ok("Workers successfully updated");
    }
    //test 
}
