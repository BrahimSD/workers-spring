package demo.controller;

import demo.model.Worker;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
