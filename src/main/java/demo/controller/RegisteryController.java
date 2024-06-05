package demo.controller;

import demo.model.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;


@RestController
@RequestMapping("/workers")
public class RegisteryController {
    @Autowired
    private WorkerRepository workersRepo;

    @Transactional

    @GetMapping("/users")
    public ResponseEntity<List<Worker>> getUsers() {
        if (!"registery".equals(System.getenv("APP_TYPE"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        }

        List<Worker> workers;
        try {
            workers = workersRepo.streamAllBy().collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to retrieve workers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }

        return ResponseEntity.ok(workers);
    }


    @PostMapping("/manifest")
public ResponseEntity<String> registerOrUpdateWorker(@RequestBody Worker worker) {
    if (!"registery".equals(System.getenv("APP_TYPE"))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Operation not allowed: This service is not configured as a registry.");
    }

    System.out.println("Manifestation received from '" + worker.getHostname() + "'.");

    Worker savedWorker = workersRepo.findById(worker.getHostname())
        .map(existingWorker -> {
            existingWorker.setLastManifestTime(LocalDateTime.now());
            System.out.println("Existing worker updated. Last manifest time set to now.");
            return workersRepo.save(existingWorker);
        })
        .orElseGet(() -> {
            worker.setLastManifestTime(LocalDateTime.now());
            System.out.println(worker.getHostname() + " registered in the database.");
            return workersRepo.save(worker);
        });

    // manageAndSyncWorkers();

    return ResponseEntity.ok("Manifestation successfully processed for " + savedWorker.getHostname());
}


    // @Scheduled(fixedDelay = 60000) // Exécuté toutes les minutes
    // public void removeUnresponsiveWorkers() {
    //     LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
    //     workersRepo.findAll().forEach(worker -> {
    //         if (worker.getLastManifestTime() == null || worker.getLastManifestTime().isBefore(twoMinutesAgo)) {
    //             workersRepo.delete(worker);
    //             System.out.println("Worker supprimé pour non-réponse : " + worker.getHostname());
    //         }
    //     });
    // }

@Transactional
@Scheduled(fixedRate = 120000)
public void manageAndSyncWorkers() {
    if (!"registery".equals(System.getenv("APP_TYPE"))) {
        System.out.println("Operation aborted: Service not configured as a registry.");
        return;
    }

    List<Worker> workers = ((Collection<Worker>) workersRepo.findAll()).stream()
        .filter(worker -> worker.getLastManifestTime().isBefore(LocalDateTime.now().minusMinutes(2)))
        .peek(worker -> {
            workersRepo.delete(worker);
            System.out.println("Unresponsive worker '" + worker.getHostname() + "' removed from the database.");
        })
        .collect(Collectors.toList());

    // Send the remaining workers' list to the load balancer
    try {
        RestClient restClient = RestClient.create();
        restClient.post()
                  .uri("http://loadbalancer:8081/postworkers")
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(workers)
                  .retrieve();
        System.out.println("Updated workers list sent to the load balancer.");
    } catch (Exception e) {
        System.err.println("Failed to send workers list to the load balancer: " + e.getMessage());
    }
}

}
