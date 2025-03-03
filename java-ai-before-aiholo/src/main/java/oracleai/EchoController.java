package oracleai;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/echo")
public class EchoController {
    private String theValue = "init";

    @GetMapping("/set")
    public String setValue(@RequestParam("value") String value) {
        theValue = value;
        System.out.println("EchoController set: " + theValue);
        return "set successfully: " + theValue;
    }

    @GetMapping("/get")
    public String getValue() {
        System.out.println("EchoController get: " + theValue);
        return theValue;
    }
}
