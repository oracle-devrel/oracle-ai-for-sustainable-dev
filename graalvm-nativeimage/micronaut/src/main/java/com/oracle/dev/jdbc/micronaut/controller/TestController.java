package com.oracle.dev.jdbc.micronaut.controller;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;


@Controller("/test")
@ExecuteOn(TaskExecutors.IO)
class TestController {


}