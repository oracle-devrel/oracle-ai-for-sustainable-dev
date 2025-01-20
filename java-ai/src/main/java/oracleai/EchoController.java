package oracleai;


import com.oracle.bmc.aivision.model.FaceDetectionFeature;
import oracleai.services.OracleVisionAI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import oracleai.services.ORDSCalls;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/echo")
public class EchoController {
    String theValue;

    @GetMapping("/set")
    public String echo(String value) {
        theValue = value;
        System.out.println("EchoController set:" + theValue);
        return "set successfully:" + theValue;
    }

    @GetMapping("/get")
    public String echo() {
        System.out.println("EchoController get:" + theValue);
        return theValue;
    }

}