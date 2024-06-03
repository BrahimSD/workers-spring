package demo.controller;

import demo.model.Worker;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@RestController
@RequestMapping("/workers")
public class RegisteryController {
    @Autowired
    private WorkerRepository workersRepo;

    @Transactional
    @GetMapping()
    public ResponseEntity<Object> getUsers() {
        Stream<Worker> s = workersRepo.streamAllBy();
        return new ResponseEntity<>(s.toList(), HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<Worker> put(@RequestBody Worker user) {
        workersRepo.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/manifest")
    public ResponseEntity<String> registerOrUpdateWorker(@RequestBody Worker workerDetails) {
        Worker workerToUpdate = workersRepo.findById(workerDetails.getHostname())
                                        .orElse(workerDetails);

        workerToUpdate.setLastManifestTime(LocalDateTime.now());
        workersRepo.save(workerToUpdate);

        String responseMessage = workerToUpdate == workerDetails ? "Nouveau worker enregistré." : "Worker mis à jour.";
        return ResponseEntity.ok(responseMessage);
    }

    @Scheduled(fixedDelay = 60000) // Exécuté toutes les minutes
    public void removeUnresponsiveWorkers() {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        workersRepo.findAll().forEach(worker -> {
            if (worker.getLastManifestTime() == null || worker.getLastManifestTime().isBefore(twoMinutesAgo)) {
                workersRepo.delete(worker);
                System.out.println("Worker supprimé pour non-réponse : " + worker.getHostname());
            }
        });
    }
}
