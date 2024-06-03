package demo.controller;

import demo.model.Worker;
import org.springframework.data.repository.CrudRepository;

import java.util.stream.Stream;

public interface WorkerRepository extends CrudRepository<Worker, String> {
    Stream<Worker> streamAllBy();
}
