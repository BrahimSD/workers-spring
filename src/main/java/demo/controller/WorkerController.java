package demo.controller;

import demo.model.Worker;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

@Controller
public class WorkerController {
    private String hostname;
    private Worker self;

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup(){
        this.hostname = System.getenv().get("HOSTNAME");
        if (this.hostname!= null){
            this.self = new Worker(hostname);
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri("http://registery:8081/workers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(this.self).retrieve();
        }
    }
    @GetMapping("/hello2")
    public ResponseEntity<String> hello(){
        return new ResponseEntity<>(hostname + " says hello!", HttpStatus.OK);
    }
}
