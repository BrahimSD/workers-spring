package demo.controller;


import demo.model.Worker;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@CrossOrigin

@Controller
public class LoadBalancer {
    private List<Worker> workers;

    private List<Worker> hello_workers = new ArrayList<>();
    private List<Worker> chat_workers = new ArrayList<>();
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

    @GetMapping("/service/chat")
    public ResponseEntity<String> chat() {
        if (!"loadbalancer".equals(System.getenv("APP_TYPE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: This service is not configured as a Load Balancer");
        }

        if (workers == null || workers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No chat workers available");
        }

        synchronized (this) { 
            Worker selectedWorker = workers.get(index);
            index = ThreadLocalRandom.current().nextInt(workers.size()); 

            RestClient restClient = RestClient.create();
            try {
                String result = restClient.get()
                                          .uri("http://" + selectedWorker.getHostname() + ":8081/chat")
                                          .retrieve()
                                          .body(String.class);
                
                System.out.println("Request sent to: " + selectedWorker.getHostname());
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                System.err.println("Error contacting worker at " + selectedWorker.getHostname());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to contact chat service");
            }
        }
    }

    @PostMapping("/postworkers")
    public ResponseEntity<String> postWorkers(@RequestBody List<Worker> workers) {
        if (!"loadbalancer".equals(System.getenv("APP_TYPE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Service is not configured as a load balancer.");
        }
        if (workers == null || workers.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid worker list: Cannot be null or empty.");
        }
        hello_workers.clear();
        chat_workers.clear();
        for (Worker worker : workers) {
            if ("hello".equals(worker.getService())) {
                hello_workers.add(worker);
            } else if ("chat".equals(worker.getService())) {
                chat_workers.add(worker);
            }
        }
        System.out.println("Workers list successfully updated with " + workers.size() + " workers. Hello workers: " + hello_workers.size() + ", Chat workers: " + chat_workers.size());
        return ResponseEntity.ok("Workers successfully updated");
    }
    
}
